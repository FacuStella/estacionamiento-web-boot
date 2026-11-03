package com.estacionamiento.web.controller;

import com.estacionamiento.web.api.ApiClient;
import com.estacionamiento.web.api.ApiException;
import com.estacionamiento.web.modelo.Accion;
import com.estacionamiento.web.modelo.Campo;
import com.estacionamiento.web.modelo.Columna;
import com.estacionamiento.web.modelo.Entidad;
import com.estacionamiento.web.modelo.Entidades;
import com.estacionamiento.web.modelo.Fila;
import com.estacionamiento.web.modelo.Lote;
import com.estacionamiento.web.modelo.Opcion;
import com.estacionamiento.web.modelo.PlayaPlanta;
import com.estacionamiento.web.modelo.PlayaZona;
import com.estacionamiento.web.util.Csv;
import com.estacionamiento.web.util.Valores;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Pantallas del ADMINISTRADOR. Un mismo controlador sirve a todas las entidades segun su descripcion en Entidades. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ApiClient api;

    public AdminController(ApiClient api) {
        this.api = api;
    }

    // ------------------------------------------------------------ playa
    /** Cantidad de colores de fondo distintos para las zonas (clases zona-color-0 a zona-color-5 en playa.css). */
    private static final int COLORES_DE_ZONA = 6;

    /** Dibuja una planta de la playa. Sin "planta" se muestra la planta baja (o la primera que haya). */
    @GetMapping("/playa")
    public String playa(@RequestParam(required = false) Integer planta, Model m) {
        List<Map<String, Object>> zonas = api.lista("admin/zonas/listar");
        List<Map<String, Object>> garages = api.lista("admin/garages/listar");
        Map<String, String> patentePorGarage = new HashMap<>();
        for (Map<String, Object> o : api.lista("admin/ocupantes/activos")) {
            patentePorGarage.put(Valores.s(Valores.leer(o, "garage.numero")), Valores.s(Valores.leer(o, "vehiculo.patente")));
        }

        Map<Integer, List<PlayaZona>> porPlanta = new TreeMap<>();
        zonas.sort(Comparator.comparing(z -> Valores.s(z.get("letra"))));
        for (int i = 0; i < zonas.size(); i++) {
            Map<String, Object> z = zonas.get(i);
            String letra = Valores.s(z.get("letra"));
            List<Lote> lotes = garages.stream()
                    .filter(g -> letra.equals(Valores.s(Valores.leer(g, "zona.letra"))))
                    .sorted(Comparator.comparingInt(g -> ((Number) g.get("numero")).intValue()))
                    .map(g -> lote(g, patentePorGarage))
                    .toList();
            String tipo = Valores.s(z.get("tipoVehiculos"));
            String tamano = tamanoCochera(tipo);
            PlayaZona zona = new PlayaZona(letra, tipo, Valores.s(z.get("tarifaHora")),
                    "zona-color-" + (i % COLORES_DE_ZONA), tamano, modulos(lotes, maximoPorHilera(tamano)));
            int numeroPlanta = z.get("planta") instanceof Number n ? n.intValue() : 0;
            porPlanta.computeIfAbsent(numeroPlanta, k -> new ArrayList<>()).add(zona);
        }

        List<PlayaPlanta> plantas = new ArrayList<>();
        porPlanta.forEach((numero, zonasDeLaPlanta) -> {
            List<Lote> lotes = zonasDeLaPlanta.stream().flatMap(z -> z.modulos().stream())
                    .flatMap(md -> Stream.concat(md.arriba().stream(), md.abajo().stream())).toList();
            plantas.add(new PlayaPlanta(numero, PlayaPlanta.nombre(numero),
                    lotes.stream().filter(Lote::disponible).count(), lotes.size(), zonasDeLaPlanta));
        });
        PlayaPlanta actual = plantas.stream().filter(p -> planta != null && p.numero() == planta).findFirst()
                .or(() -> plantas.stream().filter(p -> p.numero() == 0).findFirst())
                .orElse(plantas.isEmpty() ? null : plantas.get(0));

        m.addAttribute("plantas", plantas);
        m.addAttribute("actual", actual);
        m.addAttribute("total", garages.size());
        m.addAttribute("ocupados", patentePorGarage.size());
        m.addAttribute("libres", plantas.stream().mapToLong(PlayaPlanta::libres).sum());
        return "playa";
    }

    private static Lote lote(Map<String, Object> g, Map<String, String> patentePorGarage) {
        String numero = Valores.s(g.get("numero"));
        Object socio = Valores.leer(g, "socio.nombre");
        if (patentePorGarage.containsKey(numero)) {
            return new Lote(numero, "ocupado", "Cochera " + numero + " · ocupada por " + patentePorGarage.get(numero)
                    + ": clic para registrar la salida");
        }
        if (socio != null) {
            return new Lote(numero, "abono", "Cochera " + numero + " · abono de " + socio
                    + ": clic para que entre uno de sus vehículos");
        }
        return new Lote(numero, "libre", "Cochera " + numero + " · libre: clic para registrar una entrada");
    }

    /** Las motos usan cocheras chicas y las camionetas grandes; el resto, de auto. */
    private static String tamanoCochera(String tipoVehiculos) {
        String t = tipoVehiculos.toLowerCase();
        return t.contains("moto") ? "cochera-moto" : t.contains("camion") ? "cochera-camioneta" : "cochera-auto";
    }

    private static int maximoPorHilera(String tamano) {
        return switch (tamano) {
            case "cochera-moto" -> 20;
            case "cochera-camioneta" -> 6;
            default -> 10;
        };
    }

    /** Reparte las cocheras en dos hileras enfrentadas por modulo (o mas modulos si no entran). */
    private static List<PlayaZona.Modulo> modulos(List<Lote> lotes, int maximo) {
        int porHilera = Math.max(1, Math.min(maximo, (lotes.size() + 1) / 2));
        List<List<Lote>> hileras = new ArrayList<>();
        for (int i = 0; i < lotes.size(); i += porHilera) {
            hileras.add(lotes.subList(i, Math.min(i + porHilera, lotes.size())));
        }
        List<PlayaZona.Modulo> modulos = new ArrayList<>();
        for (int i = 0; i < hileras.size(); i += 2) {
            modulos.add(new PlayaZona.Modulo(hileras.get(i), i + 1 < hileras.size() ? hileras.get(i + 1) : List.of()));
        }
        return modulos;
    }

    /** Vuelve a la playa mostrando la planta en la que se estaba trabajando. */
    private static String volverALaPlaya(String planta) {
        return planta == null || !planta.matches("-?\\d+") ? "redirect:/admin/playa" : "redirect:/admin/playa?planta=" + planta;
    }

    /**
     * Al tocar una cochera de la playa: si esta ocupada, pantalla para registrar la salida;
     * si no, pantalla para registrar la entrada de un vehiculo.
     */
    @GetMapping("/playa/{numero}")
    public String cocheraDeLaPlaya(@PathVariable String numero, @RequestParam(required = false) String idTipo, Model m) {
        Map<String, Object> garage = api.objeto("admin/garages/mostrar/" + numero);
        List<Map<String, Object>> activos = api.lista("admin/ocupantes/activos");
        m.addAttribute("numero", numero);
        m.addAttribute("zona", Valores.s(Valores.leer(garage, "zona.letra")));
        m.addAttribute("tipoVehiculos", Valores.s(Valores.leer(garage, "zona.tipoVehiculos")));
        m.addAttribute("tarifa", Valores.s(Valores.leer(garage, "zona.tarifaHora")));
        Object planta = Valores.leer(garage, "zona.planta");
        m.addAttribute("planta", Valores.s(planta));
        m.addAttribute("nombrePlanta", planta instanceof Number n ? PlayaPlanta.nombre(n.intValue()) : "");

        Map<String, Object> ocupante = activos.stream()
                .filter(o -> numero.equals(Valores.s(Valores.leer(o, "garage.numero"))))
                .findFirst().orElse(null);
        if (ocupante != null) {
            m.addAttribute("patente", Valores.s(Valores.leer(ocupante, "vehiculo.patente")));
            m.addAttribute("vehiculo", Valores.s(Valores.leer(ocupante, "vehiculo.marca.marca")) + " · "
                    + Valores.s(Valores.leer(ocupante, "vehiculo.tipo.tipo")));
            m.addAttribute("socio", Valores.s(Valores.leer(ocupante, "vehiculo.propietario.nombre")));
            m.addAttribute("entrada", Valores.fechaHora(Valores.s(ocupante.get("entrada"))));
            return "playa-salida";
        }

        // Puede entrar cualquier vehiculo que no este adentro; si la cochera tiene abono, solo los del socio.
        Set<String> adentro = new HashSet<>();
        activos.forEach(o -> adentro.add(Valores.s(Valores.leer(o, "vehiculo.patente"))));
        Object dniAbono = Valores.leer(garage, "socio.dni");
        List<Opcion> vehiculos = api.lista("admin/vehiculos/listar").stream()
                .filter(v -> !adentro.contains(Valores.s(v.get("patente"))))
                .filter(v -> dniAbono == null || Valores.s(dniAbono).equals(Valores.s(Valores.leer(v, "propietario.dni"))))
                .sorted(Comparator.comparing(v -> Valores.s(v.get("patente"))))
                .map(v -> new Opcion(Valores.s(v.get("patente")), Valores.s(v.get("patente")) + " · " + Valores.s(Valores.leer(v, "marca.marca"))
                        + " · " + Valores.s(Valores.leer(v, "tipo.tipo")) + (Valores.leer(v, "propietario") == null ? " · de paso"
                        : " · socio " + Valores.s(Valores.leer(v, "propietario.nombre")))))
                .toList();
        m.addAttribute("abono", dniAbono == null ? null : Valores.s(Valores.leer(garage, "socio.nombre")));
        m.addAttribute("opciones", vehiculos);
        // Vehiculo nuevo: el tipo elegido o, si no, el que sugiere la zona ("Autos" -> Auto); las marcas son las de ese tipo.
        List<Opcion> tipos = Entidades.tiposVehiculo(api);
        String tipoZona = Valores.s(Valores.leer(garage, "zona.tipoVehiculos"));
        String idTipoNuevo = tipos.stream().filter(t -> t.valor().equals(idTipo)).findFirst()
                .or(() -> tipos.stream().filter(t -> tipoZona.startsWith(t.etiqueta())).findFirst())
                .or(() -> tipos.stream().findFirst())
                .map(Opcion::valor).orElse(null);
        m.addAttribute("tipos", tipos);
        m.addAttribute("idTipoNuevo", idTipoNuevo);
        m.addAttribute("marcas", idTipoNuevo == null ? List.of() : Entidades.marcasDelTipo(api, idTipoNuevo));
        m.addAttribute("socios", dniAbono == null
                ? Opcion.listar(api, "admin/socios/listar", "dni", s -> Valores.s(s.get("nombre")) + " (" + Valores.s(s.get("dni")) + ")")
                : List.of());
        return "playa-entrada";
    }

    /** Entrada de un vehiculo ya registrado (patente) o de uno nuevo, que se registra en el momento. */
    @PostMapping("/playa/{numero}/entrada")
    public String registrarEntrada(@PathVariable String numero, @RequestParam Map<String, String> p, RedirectAttributes ra) {
        String patente = p.getOrDefault("patente", "").trim().toUpperCase();
        try {
            if (p.containsKey("nuevo")) {
                Map<String, Object> vehiculo = new LinkedHashMap<>();
                vehiculo.put("patente", patente);
                if (!p.getOrDefault("idMarca", "").isBlank()) {
                    vehiculo.put("idMarca", Long.valueOf(p.get("idMarca")));
                }
                if (!p.getOrDefault("idTipo", "").isBlank()) {
                    vehiculo.put("idTipo", Long.valueOf(p.get("idTipo")));
                }
                vehiculo.put("dimensiones", p.getOrDefault("dimensiones", "").trim());
                if (!p.getOrDefault("dniPropietario", "").isBlank()) {
                    vehiculo.put("dniPropietario", Integer.valueOf(p.get("dniPropietario")));
                }
                api.enviar(HttpMethod.POST, "admin/vehiculos/registrar", vehiculo);
            }
            api.enviar(HttpMethod.POST, "admin/ocupantes/entrada/" + numero + "/" + patente, null);
            ra.addFlashAttribute("ok", "Entró el vehículo " + patente + " a la cochera " + numero + ".");
        } catch (ApiException ex) {
            if (ex.getEstado() == 401) {
                throw ex;
            }
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return volverALaPlaya(p.get("planta"));
    }

    @PostMapping("/playa/{numero}/salida")
    public String registrarSalida(@PathVariable String numero, @RequestParam(required = false) String planta,
                                  RedirectAttributes ra) {
        try {
            Map<String, Object> o = api.objeto(HttpMethod.POST, "admin/ocupantes/salida/" + numero);
            String descuento = o.get("descuento") instanceof Number d && d.doubleValue() > 0
                    ? " (descuento " + Valores.celda(d) + "%)" : "";
            ra.addFlashAttribute("ok", "Salió el vehículo " + Valores.s(Valores.leer(o, "vehiculo.patente")) + " de la cochera "
                    + numero + ": " + Valores.s(o.get("horas")) + " h, a cobrar $" + Valores.s(o.get("importe")) + descuento + ".");
        } catch (ApiException ex) {
            if (ex.getEstado() == 401) {
                throw ex;
            }
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return volverALaPlaya(planta);
    }

    // ------------------------------------------------------------ estadias
    /**
     * Listado de estadias. Con "orden" (indice de la columna) se ordena por esa columna; "dir" es
     * "desc" (de mayor a menor, por defecto) o "asc". Sin orden queda como lo manda el backend: mas nuevas primero.
     */
    @GetMapping("/estadias")
    public String estadias(@RequestParam(required = false) Integer orden,
                           @RequestParam(defaultValue = "desc") String dir, Model m) {
        List<Columna> columnas = Entidades.C_ESTADIAS;
        List<Map<String, Object>> datos = new ArrayList<>(api.lista("admin/ocupantes/listar"));
        boolean ascendente = "asc".equals(dir);
        boolean ordenValido = orden != null && orden >= 0 && orden < columnas.size();
        if (ordenValido) {
            String ruta = columnas.get(orden).ruta();
            // Se ordena por el valor crudo del JSON (no por el texto formateado) y los vacios van siempre al final.
            datos.sort((x, y) -> {
                Object a = Valores.leer(x, ruta);
                Object b = Valores.leer(y, ruta);
                if (a == null || b == null) {
                    return a == null ? (b == null ? 0 : 1) : -1;
                }
                int c = Valores.comparar(a, b);
                return ascendente ? c : -c;
            });
        }
        m.addAttribute("columnas", columnas);
        m.addAttribute("filas", datos.stream().map(x -> Fila.de(x, columnas, null)).toList());
        m.addAttribute("orden", ordenValido ? orden : -1);
        m.addAttribute("dir", ascendente ? "asc" : "desc");
        return "estadias";
    }
    
    @GetMapping("/estadias.csv")
    public ResponseEntity<byte[]> exportarEstadias() {
    List<Fila> filas = api.lista("admin/ocupantes/listar").stream()
            .map(x -> Fila.de(x, Entidades.C_ESTADIAS, null)).toList();
    String csv = Csv.armar(Entidades.C_ESTADIAS, filas);   // paso 3
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"estadias.csv\"")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(csv.getBytes(StandardCharsets.UTF_8));
}

    // ------------------------------------------------------------ listado
    @GetMapping("/{ent}")
    public String lista(@PathVariable String ent, @RequestParam(defaultValue = "") String q, Model m) {
        Entidad e = entidad(ent);
        String filtro = q.trim().toLowerCase();
        List<Fila> filas = api.lista(e.base() + "/listar").stream()
                .map(x -> Fila.de(x, e.columnas(), e.clave()))
                .filter(f -> filtro.isEmpty() || String.join(" ", f.celdas()).toLowerCase().contains(filtro))
                .toList();
        m.addAttribute("ent", e);
        m.addAttribute("filas", filas);
        m.addAttribute("q", q);
        return "lista";
    }

    // ------------------------------------------------------------ alta y edicion
    /**
     * Alta. Los parametros de la URL traen lo ya cargado cuando el formulario va en dos pasos
     * (por ejemplo, el tipo de vehiculo elegido en el paso 1); "cambiar" vuelve al paso 1.
     */
    @GetMapping("/{ent}/nuevo")
    public String nuevo(@PathVariable String ent, @RequestParam Map<String, String> p, Model m) {
        Entidad e = entidad(ent);
        return formulario(m, e, delFormulario(e, p), false, null, null, p.containsKey("cambiar"));
    }

    @PostMapping("/{ent}/guardar")
    public String guardar(@PathVariable String ent, @RequestParam Map<String, String> p, Model m, RedirectAttributes ra) {
        Entidad e = entidad(ent);
        try {
            api.enviar(HttpMethod.POST, e.base() + "/registrar", cuerpo(e, p, false));
            ra.addFlashAttribute("ok", "Se creó " + e.singular() + ".");
            return "redirect:/admin/" + ent;
        } catch (NumberFormatException ex) {
            return formulario(m, e, p, false, null, "Hay un valor numérico inválido.");
        } catch (ApiException ex) {
            if (ex.getEstado() == 401) {
                throw ex;
            }
            return formulario(m, e, p, false, null, ex.getMessage());
        }
    }

    @GetMapping("/{ent}/{clave}/editar")
    public String editar(@PathVariable String ent, @PathVariable String clave, @RequestParam Map<String, String> p, Model m) {
        Entidad e = entidad(ent);
        Map<String, Object> fila = api.objeto(e.base() + "/mostrar/" + clave);
        Map<String, String> valores = new HashMap<>();
        for (Campo c : e.campos()) {
            Object v = c.origen() != null ? Valores.leer(fila, c.origen()) : fila.get(c.nombre());
            valores.put(c.nombre(), c.tipo() == Campo.Tipo.CLAVE ? "" : Valores.s(v));
        }
        // Si viene de un paso del formulario, lo cargado en pantalla manda sobre lo guardado.
        valores.putAll(delFormulario(e, p));
        return formulario(m, e, valores, true, clave, null, p.containsKey("cambiar"));
    }

    @PostMapping("/{ent}/{clave}/actualizar")
    public String actualizar(@PathVariable String ent, @PathVariable String clave, @RequestParam Map<String, String> p,
                             Model m, RedirectAttributes ra) {
        Entidad e = entidad(ent);
        try {
            api.enviar(HttpMethod.PUT, e.base() + "/actualizar/" + clave, cuerpo(e, p, true));
            ra.addFlashAttribute("ok", "Se guardaron los cambios.");
            return "redirect:/admin/" + ent;
        } catch (NumberFormatException ex) {
            return formulario(m, e, p, true, clave, "Hay un valor numérico inválido.");
        } catch (ApiException ex) {
            if (ex.getEstado() == 401) {
                throw ex;
            }
            return formulario(m, e, p, true, clave, ex.getMessage());
        }
    }

    // ------------------------------------------------------------ acciones (asignar, quitar, eliminar, listar)
    @GetMapping("/{ent}/{clave}/accion/{id}")
    public String accion(@PathVariable String ent, @PathVariable String clave, @PathVariable String id, Model m) {
        Entidad e = entidad(ent);
        Accion a = accion(e, id);
        Map<String, Object> fila = api.objeto(e.base() + "/mostrar/" + clave);
        m.addAttribute("ent", e);
        m.addAttribute("clave", clave);
        m.addAttribute("accion", a);
        if (a.tipo() == Accion.Tipo.SELECCIONAR) {
            m.addAttribute("opciones", Opcion.listar(api, a.origen().apply(fila), a.claveValor(), a.etiquetaOpcion()));
            return "seleccion";
        }
        if (a.tipo() == Accion.Tipo.CONFIRMAR) {
            return "confirmar";
        }
        m.addAttribute("titulo", a.etiqueta() + " · " + e.singular() + " " + clave);
        m.addAttribute("columnas", a.columnas());
        m.addAttribute("filas", api.lista(a.origen().apply(fila)).stream().map(x -> Fila.de(x, a.columnas(), null)).toList());
        return "resultado";
    }

    @PostMapping("/{ent}/{clave}/accion/{id}")
    public String ejecutar(@PathVariable String ent, @PathVariable String clave, @PathVariable String id,
                           @RequestParam(required = false) String valor, RedirectAttributes ra) {
        Entidad e = entidad(ent);
        Accion a = accion(e, id);
        try {
            Map<String, Object> fila = api.objeto(e.base() + "/mostrar/" + clave);
            api.enviar(a.metodo(), a.destino().apply(fila, valor), null);
            ra.addFlashAttribute("ok", a.etiqueta() + ": listo.");
        } catch (ApiException ex) {
            if (ex.getEstado() == 401) {
                throw ex;
            }
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/" + ent;
    }

    // ------------------------------------------------------------ auxiliares
    private Entidad entidad(String id) {
        Entidad e = Entidades.buscar(id);
        if (e == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return e;
    }

    private Accion accion(Entidad e, String id) {
        if ("eliminar".equals(id)) {
            return e.eliminar();
        }
        return e.acciones().stream().filter(a -> a.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private String formulario(Model m, Entidad e, Map<String, String> valores, boolean editar, String clave, String error) {
        return formulario(m, e, valores, editar, clave, error, false);
    }

    /**
     * Si algun desplegable depende de otro campo (la marca del tipo de vehiculo), el formulario va en dos pasos:
     * paso 1, elegir ese otro campo; paso 2, el resto, con las opciones ya filtradas.
     */
    private String formulario(Model m, Entidad e, Map<String, String> datos, boolean editar, String clave, String error,
                              boolean volverAlPaso1) {
        Map<String, String> valores = new HashMap<>(datos);
        Map<String, List<Opcion>> opciones = new LinkedHashMap<>();
        for (Campo c : e.campos()) {
            if (c.opciones() != null) {
                opciones.put(c.nombre(), c.opciones().apply(api));
            }
        }
        Set<String> padres = new HashSet<>();
        boolean paso1 = volverAlPaso1;
        for (Campo c : e.campos()) {
            if (c.dependeDe() == null) {
                continue;
            }
            padres.add(c.dependeDe());
            String elegido = valores.getOrDefault(c.dependeDe(), "");
            if (!elegido.matches("\\d+")) {
                paso1 = true;
                continue;
            }
            List<Opcion> lista = c.opcionesSegun().apply(api, elegido);
            opciones.put(c.nombre(), lista);
            // si lo elegido antes no corresponde al nuevo valor (otra marca para otro tipo), se vuelve a elegir
            String actual = valores.getOrDefault(c.nombre(), "");
            if (lista.stream().noneMatch(o -> o.valor().equals(actual))) {
                valores.put(c.nombre(), "");
            }
        }
        // texto de lo elegido en el paso 1, para mostrarlo en el paso 2
        Map<String, String> elegidos = new HashMap<>();
        for (String padre : padres) {
            String v = valores.getOrDefault(padre, "");
            opciones.getOrDefault(padre, List.of()).stream().filter(o -> o.valor().equals(v)).findFirst()
                    .ifPresent(o -> elegidos.put(padre, o.etiqueta()));
        }
        m.addAttribute("paso1", paso1 && !padres.isEmpty());
        m.addAttribute("padres", padres);
        m.addAttribute("elegidos", elegidos);
        m.addAttribute("ent", e);
        m.addAttribute("campos", e.campos());
        m.addAttribute("valores", valores);
        m.addAttribute("opciones", opciones);
        m.addAttribute("editar", editar);
        m.addAttribute("clave", clave);
        m.addAttribute("error", error);
        return "formulario";
    }

    /** Se queda solo con los parametros que son campos de la entidad (descarta "cambiar" y otros). */
    private static Map<String, String> delFormulario(Entidad e, Map<String, String> p) {
        Map<String, String> valores = new HashMap<>();
        for (Campo c : e.campos()) {
            if (p.containsKey(c.nombre()) && c.tipo() != Campo.Tipo.CLAVE) {
                valores.put(c.nombre(), p.get(c.nombre()));
            }
        }
        return valores;
    }

    /** Arma el JSON que espera el backend a partir de los datos del formulario. */
    private Map<String, Object> cuerpo(Entidad e, Map<String, String> p, boolean editando) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (Campo c : e.campos()) {
            if (editando && c.soloCrear()) {
                continue;
            }
            if (c.esCheck()) {
                body.put(c.nombre(), p.containsKey(c.nombre()));
                continue;
            }
            String v = p.getOrDefault(c.nombre(), "").trim();
            if (v.isEmpty()) {
                continue;
            }
            body.put(c.nombre(), switch (c.tipo()) {
                case NUMERO, SELECT_NUMERO -> Integer.valueOf(v);
                case DECIMAL -> Double.valueOf(v.replace(',', '.'));
                default -> v;
            });
        }
        return body;
    }
}
