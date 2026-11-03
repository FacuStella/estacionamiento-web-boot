package com.estacionamiento.web.controller;

import com.estacionamiento.web.api.ApiClient;
import com.estacionamiento.web.modelo.Columna;
import com.estacionamiento.web.modelo.Entidades;
import com.estacionamiento.web.modelo.Fila;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Pantallas de solo lectura de EMPLEADO y SOCIO: siempre sobre sus propios datos (lo resuelve el backend con el token). */
@Controller
public class PanelController {

    private final ApiClient api;

    public PanelController(ApiClient api) {
        this.api = api;
    }

    @GetMapping("/empleado/vehiculos")
    public String empleadoVehiculos(Model m) {
        return tabla(m, "Vehículos", "empleado/listarVehiculos", Entidades.C_VEHICULOS);
    }

    @GetMapping("/empleado/garages")
    public String empleadoGarages(Model m) {
        return tabla(m, "Garages", "empleado/listarGarages", Entidades.C_GARAGES);
    }

    @GetMapping("/empleado/zonas")
    public String empleadoZonas(Model m) {
        return tabla(m, "Zonas", "empleado/listarZonas", Entidades.C_ZONAS);
    }

    @GetMapping("/socio/perfil")
    public String perfil(Model m) {
        m.addAttribute("s", api.objeto("socio/perfil"));
        return "perfil";
    }

    @GetMapping("/socio/vehiculos")
    public String socioVehiculos(Model m) {
        return tabla(m, "Mis vehículos", "socio/listarVehiculos", Entidades.C_VEHICULOS);
    }

    @GetMapping("/socio/garages")
    public String socioGarages(Model m) {
        return tabla(m, "Mis garages", "socio/listarGarages", Entidades.C_GARAGES);
    }

    private String tabla(Model m, String titulo, String path, List<Columna> columnas) {
        m.addAttribute("titulo", titulo);
        m.addAttribute("columnas", columnas);
        m.addAttribute("filas", api.lista(path).stream().map(x -> Fila.de(x, columnas, null)).toList());
        return "tabla";
    }
}
