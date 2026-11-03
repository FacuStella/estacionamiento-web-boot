package com.estacionamiento.web.util;

import com.estacionamiento.web.modelo.Columna;
import com.estacionamiento.web.modelo.Fila;
import java.util.List;
import static java.util.stream.Collectors.joining;

public final class Csv {

    public static String armar(List<Columna> columnas, List<Fila> filas) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append(columnas.stream().map(c -> celda(c.titulo())).collect(joining(";"))).append("\r\n");
        for (Fila f : filas) {
            sb.append(f.celdas().stream().map(Csv::celda).collect(joining(";"))).append("\r\n");
        }
        return sb.toString();
    }

    private static String celda(String v) {
        if (v.equals("—")) return "";                       // Valores.celda usa "—" para vacío
        return v.matches(".*[;\"\\r\\n].*") ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }

}