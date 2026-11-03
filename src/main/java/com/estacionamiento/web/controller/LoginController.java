package com.estacionamiento.web.controller;

import com.estacionamiento.web.api.ApiClient;
import com.estacionamiento.web.api.ApiException;
import com.estacionamiento.web.sesion.SesionUsuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    private final ApiClient api;
    private final SesionUsuario sesion;

    public LoginController(ApiClient api, SesionUsuario sesion) {
        this.api = api;
        this.sesion = sesion;
    }

    @GetMapping("/")
    public String inicio() {
        if (!sesion.estaLogueado()) {
            return "redirect:/login";
        }
        if (sesion.isDebeCambiarClave()) {
            return "redirect:/cuenta/clave";
        }
        return switch (sesion.getTipoUsuario()) {
            case "ADMINISTRADOR" -> "redirect:/admin/playa";
            case "EMPLEADO" -> "redirect:/empleado/vehiculos";
            default -> "redirect:/socio/perfil";
        };
    }

    /** Volver a la pantalla de login (por ejemplo con "Atras" del navegador) cierra la sesion. */
    @GetMapping("/login")
    public String formulario(@RequestParam(required = false) String vencida, Model model, HttpSession http) {
        if (sesion.estaLogueado()) {
            sesion.cerrar();
            http.invalidate();
        }
        if (vencida != null) {
            model.addAttribute("error", "La sesión venció. Ingresá de nuevo.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String entrar(@RequestParam String username, @RequestParam String password, Model model,
                         HttpServletRequest request) {
        try {
            Map<String, Object> r = api.autenticar(username.trim(), password);
            // id de sesion nuevo al loguearse: si alguien conocia el anterior (fijacion de sesion), ya no le sirve
            request.changeSessionId();
            sesion.iniciar((String) r.get("token"), (String) r.get("tipoUsuario"), (String) r.get("username"),
                    Boolean.TRUE.equals(r.get("debeCambiarClave")));
            return "redirect:/";
        } catch (ApiException e) {
            model.addAttribute("error", e.getMessage());
            return "login";
        }
    }

    @PostMapping("/logout")
    public String salir(HttpSession http) {
        sesion.cerrar();
        http.invalidate();
        return "redirect:/login";
    }
}
