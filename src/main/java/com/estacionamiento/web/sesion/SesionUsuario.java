package com.estacionamiento.web.sesion;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/** Datos del usuario logueado. Hay una instancia por sesion HTTP. */
@Component
@SessionScope
public class SesionUsuario {

    private String token;
    private String tipoUsuario;
    private String username;
    /** Entro con una clave temporal: hasta cambiarla solo puede ir a /cuenta/clave. */
    private boolean debeCambiarClave;

    public void iniciar(String token, String tipoUsuario, String username, boolean debeCambiarClave) {
        this.token = token;
        this.tipoUsuario = tipoUsuario;
        this.username = username;
        this.debeCambiarClave = debeCambiarClave;
    }

    public void claveCambiada() {
        debeCambiarClave = false;
    }

    public void cerrar() {
        token = null;
        tipoUsuario = null;
        username = null;
        debeCambiarClave = false;
    }

    public boolean estaLogueado() {
        return token != null;
    }

    public String getToken() {
        return token;
    }

    public String getTipoUsuario() {
        return tipoUsuario;
    }

    public String getUsername() {
        return username;
    }

    public boolean isDebeCambiarClave() {
        return debeCambiarClave;
    }
}
