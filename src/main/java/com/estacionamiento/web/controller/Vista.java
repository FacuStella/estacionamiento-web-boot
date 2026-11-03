package com.estacionamiento.web.controller;

import com.estacionamiento.web.api.ApiException;
import com.estacionamiento.web.modelo.Entidad;
import com.estacionamiento.web.modelo.Entidades;
import com.estacionamiento.web.modelo.MenuItem;
import com.estacionamiento.web.sesion.SesionUsuario;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Datos que necesitan todas las paginas (menu, usuario) y manejo comun de errores. */
@ControllerAdvice
public class Vista {

    private final SesionUsuario sesion;

    public Vista(SesionUsuario sesion) {
        this.sesion = sesion;
    }

    @ModelAttribute("menu")
    public List<MenuItem> menu() {
        List<MenuItem> items = new ArrayList<>();
        if (!sesion.estaLogueado() || sesion.isDebeCambiarClave()) {
            return items;
        }
        switch (sesion.getTipoUsuario()) {
            case "ADMINISTRADOR" -> {
                items.add(new MenuItem("Playa", "/admin/playa"));
                items.add(new MenuItem("Estadías", "/admin/estadias"));
                for (Entidad e : Entidades.todas()) {
                    items.add(new MenuItem(e.titulo(), "/admin/" + e.id()));
                }
            }
            case "EMPLEADO" -> {
                items.add(new MenuItem("Vehículos", "/empleado/vehiculos"));
                items.add(new MenuItem("Garages", "/empleado/garages"));
                items.add(new MenuItem("Zonas", "/empleado/zonas"));
            }
            default -> {
                items.add(new MenuItem("Perfil", "/socio/perfil"));
                items.add(new MenuItem("Mis vehículos", "/socio/vehiculos"));
                items.add(new MenuItem("Mis garages", "/socio/garages"));
            }
        }
        return items;
    }

    @ModelAttribute("usuario")
    public String usuario() {
        return sesion.getUsername();
    }

    @ModelAttribute("uri")
    public String uri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ExceptionHandler(ApiException.class)
    public String errorDeApi(ApiException e, Model model) {
        if (e.getEstado() == 401) {
            sesion.cerrar();
            return "redirect:/login?vencida";
        }
        model.addAttribute("mensaje", e.getMessage());
        return "error";
    }
}
