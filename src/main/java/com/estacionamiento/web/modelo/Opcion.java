package com.estacionamiento.web.modelo;

import com.estacionamiento.web.api.ApiClient;
import com.estacionamiento.web.util.Valores;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Item de un desplegable (select). */
public record Opcion(String valor, String etiqueta) {

    /** Arma las opciones a partir de un listado del backend. */
    public static List<Opcion> listar(ApiClient api, String path, String clave,
                                      Function<Map<String, Object>, String> etiqueta) {
        return api.lista(path).stream()
                .map(x -> new Opcion(Valores.s(x.get(clave)), etiqueta.apply(x)))
                .toList();
    }
}
