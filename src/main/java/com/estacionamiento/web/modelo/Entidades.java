package com.estacionamiento.web.modelo;

import static com.estacionamiento.web.util.Valores.leer;
import static com.estacionamiento.web.util.Valores.s;

import com.estacionamiento.web.api.ApiClient;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.http.HttpMethod;

/** Catalogo de las entidades que administra el ADMINISTRADOR, con sus columnas, formularios y acciones. */
public final class Entidades {

    private static final Map<String, Entidad> TODAS = new LinkedHashMap<>();

    public static Entidad buscar(String id) {
        return TODAS.get(id);
    }

    public static Collection<Entidad> todas() {
        return TODAS.values();
    }

    // ---- etiquetas para los desplegables ----
    private static final Function<Map<String, Object>, String> L_ZONA = m -> s(m.get("letra")) + " · " + s(m.get("tipoVehiculos")) + " · planta " + s(m.get("planta"));
    private static final Function<Map<String, Object>, String> L_PERSONA = m -> s(m.get("nombre")) + " (" + s(m.get("dni")) + ")";
    private static final Function<Map<String, Object>, String> L_VEHICULO = m -> s(m.get("patente")) + " · " + s(leer(m, "marca.marca"));

    // ---- columnas compartidas (tambien las usan las pantallas de empleado y socio) ----
    public static final List<Columna> C_VEHICULOS = List.of(col("patente", "Patente"), col("marca.marca", "Marca"), col("tipo.tipo", "Tipo"),
            col("dimensiones", "Dimensiones"), col("propietario.nombre", "Socio"));
    public static final List<Columna> C_ESTADIAS = List.of(col("vehiculo.patente", "Patente"), col("vehiculo.propietario.nombre", "Socio"),
            col("garage.numero", "Garage"), col("garage.zona.letra", "Zona"), col("entrada", "Entrada"), col("salida", "Salida"),
            col("horas", "Horas"), col("descuento", "Descuento %"), col("importe", "Importe $"));
    public static final List<Columna> C_GARAGES = List.of(col("numero", "N°"), col("zona.letra", "Zona"), col("socio.nombre", "Abono (socio)"),
            col("lecturaContadorLuz", "Contador de luz"), col("mantenimientoContratado", "Mantenimiento"), col("fechaCompra", "Compra"));
    public static final List<Columna> C_ZONAS = List.of(col("letra", "Letra"), col("planta", "Planta"), col("tipoVehiculos", "Vehículos"),
            col("profundidadGarage", "Profundidad"), col("anchoGarage", "Ancho"), col("tarifaHora", "Tarifa por hora $"));
    private static final List<Columna> C_EMPLEADOS_ZONA = List.of(col("dni", "DNI"), col("nombre", "Nombre"), col("especialidad", "Especialidad"));

    private static Columna col(String ruta, String titulo) {
        return new Columna(ruta, titulo);
    }

    // ---- opciones de desplegables que consultan al backend ----
    private static List<Opcion> zonas(ApiClient api) {
        return Opcion.listar(api, "admin/zonas/listar", "letra", L_ZONA);
    }

    /** Auto, Moto, Camioneta: salen de la tabla tipos_vehiculo del backend. */
    public static List<Opcion> tiposVehiculo(ApiClient api) {
        return Opcion.listar(api, "admin/tiposVehiculo/listar", "idTipo", m -> s(m.get("tipo")));
    }

    /** Las marcas que fabrican vehiculos del tipo elegido (sale de la tabla marca_tipo_vehiculo del backend). */
    public static List<Opcion> marcasDelTipo(ApiClient api, String idTipo) {
        return Opcion.listar(api, "admin/marcas/listar?idTipo=" + idTipo, "idMarca", m -> s(m.get("marca")));
    }

    private static List<Opcion> socios(ApiClient api) {
        return Opcion.listar(api, "admin/socios/listar", "dni", L_PERSONA);
    }

    // ---- campos compartidos ----
    private static List<Campo> persona() {
        return List.of(Campo.texto("nombre", "Nombre").req(), Campo.numero("dni", "DNI").req().fijo(),
                Campo.texto("direccion", "Dirección"), Campo.texto("telefono", "Teléfono"));
    }

    /** La contraseña no se carga: el backend genera una temporal y la manda al mail. */
    private static List<Campo> acceso() {
        return List.of(Campo.texto("username", "Usuario").req().soloCrearA().desde("usuario.username"),
                Campo.mail("mail", "Mail (le llega la contraseña para el primer ingreso)").req().desde("usuario.mail"));
    }

    /** Nueva clave temporal por mail; el usuario la tiene que cambiar al ingresar. */
    private static Accion restablecerClave(Function<Map<String, Object>, String> username) {
        return Accion.confirmar("restablecer-clave", "Restablecer contraseña", HttpMethod.POST,
                f -> "admin/usuarios/restablecerClave/" + username.apply(f));
    }

    private static List<Campo> unir(List<Campo> a, List<Campo> b) {
        return Stream.concat(a.stream(), b.stream()).toList();
    }

    private static void registrar(Entidad e) {
        TODAS.put(e.id(), e);
    }

    static {
        registrar(new Entidad("socios", "Socios", "admin/socios", "dni", "el socio", "Nuevo socio",
                List.of(col("dni", "DNI"), col("nombre", "Nombre"), col("telefono", "Teléfono"),
                        col("usuario.username", "Usuario"), col("usuario.mail", "Mail"), col("fechaIngreso", "Ingreso")),
                unir(persona(), acceso()),
                List.of(
                        Accion.listar("vehiculos", "Vehículos", f -> "admin/socios/listarVehiculos/" + s(f.get("dni")), C_VEHICULOS),
                        Accion.listar("garages", "Garages", f -> "admin/socios/listarGarages/" + s(f.get("dni")), C_GARAGES),
                        restablecerClave(f -> s(leer(f, "usuario.username"))))));

        registrar(new Entidad("empleados", "Empleados", "admin/empleados", "dni", "el empleado", "Nuevo empleado",
                List.of(col("dni", "DNI"), col("nombre", "Nombre"), col("codigo", "Código"), col("especialidad", "Especialidad"),
                        col("telefono", "Teléfono"), col("usuario.username", "Usuario"), col("usuario.mail", "Mail")),
                unir(unir(persona(), List.of(Campo.numero("codigo", "Código").req(), Campo.texto("especialidad", "Especialidad").req())), acceso()),
                List.of(
                        Accion.seleccionar("asignar-zona", "Asignar zona", HttpMethod.POST, f -> "admin/zonas/listar", "letra", L_ZONA,
                                (f, v) -> "admin/empleados/asignarZona/" + s(f.get("dni")) + "/" + v),
                        Accion.seleccionar("quitar-zona", "Quitar zona", HttpMethod.DELETE, f -> "admin/empleados/listarZonas/" + s(f.get("dni")), "letra", L_ZONA,
                                (f, v) -> "admin/empleados/quitarZona/" + s(f.get("dni")) + "/" + v),
                        Accion.seleccionar("asignar-vehiculo", "Asignar vehículo", HttpMethod.POST, f -> "admin/vehiculos/listar", "patente", L_VEHICULO,
                                (f, v) -> "admin/empleados/asignarVehiculo/" + s(f.get("dni")) + "/" + v),
                        Accion.seleccionar("quitar-vehiculo", "Quitar vehículo", HttpMethod.DELETE, f -> "admin/empleados/listarVehiculos/" + s(f.get("dni")), "patente", L_VEHICULO,
                                (f, v) -> "admin/empleados/quitarVehiculo/" + s(f.get("dni")) + "/" + v),
                        Accion.listar("zonas", "Zonas", f -> "admin/empleados/listarZonas/" + s(f.get("dni")), C_ZONAS),
                        Accion.listar("vehiculos", "Vehículos", f -> "admin/empleados/listarVehiculos/" + s(f.get("dni")), C_VEHICULOS),
                        restablecerClave(f -> s(leer(f, "usuario.username"))))));

        registrar(new Entidad("zonas", "Zonas", "admin/zonas", "letra", "la zona", "Nueva zona", C_ZONAS,
                List.of(Campo.texto("letra", "Letra").req().fijo(), Campo.texto("tipoVehiculos", "Tipo de vehículos").req(),
                        Campo.numero("profundidadGarage", "Profundidad del garage").req(), Campo.numero("anchoGarage", "Ancho del garage").req(),
                        Campo.numero("planta", "Planta (0 = planta baja, 1 = piso 1, -1 = subsuelo 1)").req(), Campo.decimal("tarifaHora", "Tarifa por hora $").req()),
                List.of(
                        Accion.seleccionar("asignar-empleado", "Asignar empleado", HttpMethod.POST, f -> "admin/empleados/listar", "dni", L_PERSONA,
                                (f, v) -> "admin/zonas/asignarEmpleado/" + s(f.get("letra")) + "/" + v),
                        Accion.seleccionar("quitar-empleado", "Quitar empleado", HttpMethod.DELETE, f -> "admin/zonas/listarEmpleados/" + s(f.get("letra")), "dni", L_PERSONA,
                                (f, v) -> "admin/zonas/quitarEmpleado/" + s(f.get("letra")) + "/" + v),
                        Accion.listar("empleados", "Empleados", f -> "admin/zonas/listarEmpleados/" + s(f.get("letra")), C_EMPLEADOS_ZONA),
                        Accion.listar("garages", "Garages", f -> "admin/zonas/listarGarages/" + s(f.get("letra")), C_GARAGES))));

        registrar(new Entidad("garages", "Garages", "admin/garages", "numero", "el garage", "Nuevo garage", C_GARAGES,
                List.of(Campo.numero("numero", "Número").req().fijo(),
                        Campo.select("letraZona", "Zona", Entidades::zonas).req().desde("zona.letra"),
                        Campo.selectNumero("dniSocio", "Abono (socio dueño)", Entidades::socios).desde("socio.dni").conVacio("Sin abono"),
                        Campo.decimal("lecturaContadorLuz", "Lectura del contador de luz"),
                        Campo.check("mantenimientoContratado", "Mantenimiento contratado")),
                List.of(
                        Accion.seleccionar("asignar-socio", "Asignar abono", HttpMethod.PUT, f -> "admin/socios/listar", "dni", L_PERSONA,
                                (f, v) -> "admin/garages/asignarSocio/" + s(f.get("numero")) + "/" + v),
                        Accion.confirmar("quitar-socio", "Quitar abono", HttpMethod.DELETE, f -> "admin/garages/quitarSocio/" + s(f.get("numero"))))));

        registrar(new Entidad("vehiculos", "Vehículos", "admin/vehiculos", "patente", "el vehículo", "Nuevo vehículo", C_VEHICULOS,
                List.of(Campo.texto("patente", "Patente").req().fijo(),
                        Campo.selectNumero("idTipo", "Tipo", Entidades::tiposVehiculo).req().desde("tipo.idTipo"),
                        Campo.selectSegun("idMarca", "Marca", "idTipo", Entidades::marcasDelTipo).req().desde("marca.idMarca"),
                        Campo.texto("dimensiones", "Dimensiones").req(),
                        Campo.selectNumero("dniPropietario", "Socio", Entidades::socios).desde("propietario.dni").conVacio("No es de un socio (de paso)")),
                List.of(
                        Accion.listar("estadias", "Estadías", f -> "admin/ocupantes/vehiculo/" + s(f.get("patente")), C_ESTADIAS))));

        registrar(new Entidad("administradores", "Administradores", "admin/administradores", "username", "el administrador", "Nuevo administrador",
                List.of(col("username", "Usuario"), col("mail", "Mail"), col("debeCambiarClave", "Contraseña temporal")),
                List.of(Campo.texto("username", "Usuario").req().fijo(),
                        Campo.mail("mail", "Mail (le llega la contraseña para el primer ingreso)").req()),
                List.of(restablecerClave(f -> s(f.get("username"))))));
    }

    private Entidades() {
    }
}
