package com.estacionamiento.web.modelo;

import java.util.List;

/** Una planta de la playa (planta baja, piso 1, subsuelo 1...) con sus zonas. */
public record PlayaPlanta(int numero, String nombre, long libres, long total, List<PlayaZona> zonas) {

    /** 0 = planta baja, positivos = pisos, negativos = subsuelos. */
    public static String nombre(int numero) {
        if (numero == 0) {
            return "Planta baja";
        }
        return numero > 0 ? "Piso " + numero : "Subsuelo " + (-numero);
    }
}
