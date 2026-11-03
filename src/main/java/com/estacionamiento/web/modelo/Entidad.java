package com.estacionamiento.web.modelo;

import com.estacionamiento.web.util.Valores;
import java.util.List;
import org.springframework.http.HttpMethod;

/** Todo lo necesario para administrar un tipo de registro (socios, zonas, etc.). */
public record Entidad(String id, String titulo, String base, String clave, String singular, String nuevo,
                      List<Columna> columnas, List<Campo> campos, List<Accion> acciones) {

    /** Accion de borrado, comun a todas las entidades. */
    public Accion eliminar() {
        return Accion.confirmar("eliminar", "Eliminar", HttpMethod.DELETE,
                fila -> base + "/eliminar/" + Valores.s(fila.get(clave)));
    }
}
