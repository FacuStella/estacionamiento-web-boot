package com.estacionamiento.web.controller;

import com.estacionamiento.web.api.ApiClient;
import com.estacionamiento.web.api.ApiException;
import com.estacionamiento.web.sesion.SesionUsuario;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Cambio de contraseña del usuario logueado (cualquier rol). Es obligatorio si entro con una clave temporal. */
@Controller
@RequestMapping("/cuenta")
public class CuentaController {

    private final ApiClient api;
    private final SesionUsuario sesion;

    public CuentaController(ApiClient api, SesionUsuario sesion) {
        this.api = api;
        this.sesion = sesion;
    }

    @GetMapping("/clave")
    public String formulario(Model m) {
        if (!sesion.estaLogueado()) {
            return "redirect:/login";
        }
        m.addAttribute("obligatorio", sesion.isDebeCambiarClave());
        return "cuenta-clave";
    }

    @PostMapping("/clave")
    public String cambiar(@RequestParam String actual, @RequestParam String nueva, @RequestParam String repetida,
                          Model m, RedirectAttributes ra) {
        if (!sesion.estaLogueado()) {
            return "redirect:/login";
        }
        if (!nueva.equals(repetida)) {
            return conError(m, "La contraseña nueva y su repetición no coinciden.");
        }
        try {
            api.enviar(HttpMethod.POST, "cuenta/cambiarClave", Map.of("actual", actual, "nueva", nueva));
        } catch (ApiException ex) {
            if (ex.getEstado() == 401) {
                throw ex;
            }
            return conError(m, ex.getMessage());
        }
        sesion.claveCambiada();
        ra.addFlashAttribute("ok", "Se cambió tu contraseña.");
        return "redirect:/";
    }

    private String conError(Model m, String error) {
        m.addAttribute("obligatorio", sesion.isDebeCambiarClave());
        m.addAttribute("error", error);
        return "cuenta-clave";
    }
}
