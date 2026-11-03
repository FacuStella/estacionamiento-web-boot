package com.estacionamiento.web.modelo;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpMethod;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Operacion extra sobre un registro (asignar, quitar, listar relacionados).
 * Sin JavaScript, cada una es un link a una pagina propia: elegir una opcion, confirmar o ver un listado.
 */
public record Accion(String id, String etiqueta, Tipo tipo, HttpMethod metodo,
                     Function<Map<String, Object>, String> origen, String claveValor,
                     Function<Map<String, Object>, String> etiquetaOpcion,
                     BiFunction<Map<String, Object>, String, String> destino, List<Columna> columnas) {

    public enum Tipo { SELECCIONAR, CONFIRMAR, LISTAR }

    /** Muestra un desplegable con los datos de "origen"; al elegir, llama a "destino". */
    public static Accion seleccionar(String id, String etiqueta, HttpMethod metodo,
                                     Function<Map<String, Object>, String> origen, String claveValor,
                                     Function<Map<String, Object>, String> etiquetaOpcion,
                                     BiFunction<Map<String, Object>, String, String> destino) {
        return new Accion(id, etiqueta, Tipo.SELECCIONAR, metodo, origen, claveValor, etiquetaOpcion, destino, null);
    }

    /** Pide confirmacion y llama a "destino". */
    public static Accion confirmar(String id, String etiqueta, HttpMethod metodo,
                                   Function<Map<String, Object>, String> destino) {
        return new Accion(id, etiqueta, Tipo.CONFIRMAR, metodo, null, null, null, (fila, valor) -> destino.apply(fila), null);
    }

    /** Muestra una tabla con el listado que devuelve "origen". */
    public static Accion listar(String id, String etiqueta, Function<Map<String, Object>, String> origen,
                                List<Columna> columnas) {
        return new Accion(id, etiqueta, Tipo.LISTAR, HttpMethod.GET, origen, null, null, null, columnas);
    }
}
