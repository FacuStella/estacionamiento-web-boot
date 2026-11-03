package com.estacionamiento.web.modelo;

import com.estacionamiento.web.util.Valores;
import java.util.List;
import java.util.Map;

/** Una fila lista para mostrar: su clave (dni, patente...) y el texto de cada celda. */
public record Fila(String clave, List<String> celdas) {

    public static Fila de(Map<String, Object> datos, List<Columna> columnas, String campoClave) {
        return new Fila(campoClave == null ? "" : Valores.s(datos.get(campoClave)),
                columnas.stream().map(c -> Valores.celda(Valores.leer(datos, c.ruta()))).toList());
    }
}
