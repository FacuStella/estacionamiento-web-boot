package com.estacionamiento.web.api;

/** Error devuelto por el backend (o imposibilidad de conectarse a el). */
public class ApiException extends RuntimeException {

    private final int estado;

    public ApiException(int estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public int getEstado() {
        return estado;
    }
}
