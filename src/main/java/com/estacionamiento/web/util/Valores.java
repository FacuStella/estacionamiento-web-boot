package com.estacionamiento.web.util;

import java.util.Map;

/** Utilidades para leer los datos JSON (Map) que devuelve el backend. */
public final class Valores {

    private Valores() {
    }

    /** Lee una ruta con puntos, por ejemplo "zona.letra". Devuelve null si algo falta. */
    public static Object leer(Map<String, Object> mapa, String ruta) {
        Object actual = mapa;
        for (String parte : ruta.split("\\.")) {
            if (!(actual instanceof Map<?, ?> m)) {
                return null;
            }
            actual = m.get(parte);
        }
        return actual;
    }

    /** Texto crudo ("" si es null). Sirve para armar URLs y valores de formulario. */
    public static String s(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    /** Texto para mostrar en una celda de tabla. */
    public static String celda(Object o) {
        if (o == null) {
            return "—";
        }
        if (o instanceof Boolean b) {
            return b ? "Sí" : "No";
        }
        return fechaHora(String.valueOf(o));
    }

    /**
     * Compara dos valores del JSON (sin null): numeros como numeros y el resto como texto.
     * Las fechas llegan en formato ISO ("2026-11-01T19:45"), que como texto ya se ordena bien.
     */
    public static int comparar(Object a, Object b) {
        if (a instanceof Number x && b instanceof Number y) {
            return Double.compare(x.doubleValue(), y.doubleValue());
        }
        return String.valueOf(a).compareToIgnoreCase(String.valueOf(b));
    }

    /** "2026-11-01T19:45:12.3" se muestra como "01/11/2026 19:45"; cualquier otro texto queda igual. */
    public static String fechaHora(String texto) {
        if (!texto.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}.*")) {
            return texto;
        }
        return texto.substring(8, 10) + "/" + texto.substring(5, 7) + "/" + texto.substring(0, 4) + " " + texto.substring(11, 16);
    }
}
