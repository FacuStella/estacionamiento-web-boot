package com.estacionamiento.web.modelo;

import java.util.List;

/**
 * Una zona de la playa. Sus cocheras se dibujan en modulos: una hilera que mira hacia abajo,
 * la calle de circulacion y otra hilera enfrentada que mira hacia arriba.
 *
 * @param color  clase CSS con el color de fondo que identifica a la zona
 * @param tamano clase CSS con el tamaño de las cocheras (moto, auto o camioneta)
 */
public record PlayaZona(String letra, String tipo, String tarifaHora, String color, String tamano, List<Modulo> modulos) {

    /** Dos hileras enfrentadas con la calle en el medio (la de abajo puede estar vacia). */
    public record Modulo(List<Lote> arriba, List<Lote> abajo) {
    }
}
