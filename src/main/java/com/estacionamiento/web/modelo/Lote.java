package com.estacionamiento.web.modelo;

/** Una cochera dibujada en la playa. estado: "libre", "abono" (de un socio, sin vehiculo) u "ocupado". */
public record Lote(String numero, String estado, String titulo) {

    public boolean disponible() {
        return "libre".equals(estado);
    }

    public boolean ocupado() {
        return "ocupado".equals(estado);
    }
}
