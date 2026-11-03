package com.estacionamiento.web.modelo;

import com.estacionamiento.web.api.ApiClient;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Describe un campo de formulario. Se construye con los metodos estaticos y se ajusta encadenando.
 * Un desplegable puede depender de otro campo (dependeDe): sus opciones salen de opcionesSegun con el valor
 * elegido en ese otro campo, y el formulario se completa en dos pasos (primero el otro campo, despues el resto).
 */
public record Campo(String nombre, String etiqueta, Tipo tipo, boolean requerido, boolean bloqueado,
                    boolean soloCrear, String origen, Function<ApiClient, List<Opcion>> opciones, String vacio,
                    String dependeDe, BiFunction<ApiClient, String, List<Opcion>> opcionesSegun) {

    public enum Tipo { TEXTO, NUMERO, DECIMAL, CLAVE, MAIL, SELECT, SELECT_NUMERO, CHECK }

    public static Campo texto(String nombre, String etiqueta) {
        return nuevo(nombre, etiqueta, Tipo.TEXTO, null);
    }

    public static Campo numero(String nombre, String etiqueta) {
        return nuevo(nombre, etiqueta, Tipo.NUMERO, null);
    }

    public static Campo decimal(String nombre, String etiqueta) {
        return nuevo(nombre, etiqueta, Tipo.DECIMAL, null);
    }

    public static Campo clave(String nombre, String etiqueta) {
        return nuevo(nombre, etiqueta, Tipo.CLAVE, null);
    }

    public static Campo mail(String nombre, String etiqueta) {
        return nuevo(nombre, etiqueta, Tipo.MAIL, null);
    }

    public static Campo check(String nombre, String etiqueta) {
        return nuevo(nombre, etiqueta, Tipo.CHECK, null);
    }

    /** Desplegable cuyo valor es texto (por ejemplo la letra de una zona). */
    public static Campo select(String nombre, String etiqueta, Function<ApiClient, List<Opcion>> opciones) {
        return nuevo(nombre, etiqueta, Tipo.SELECT, opciones);
    }

    /** Desplegable cuyo valor es un numero (por ejemplo un DNI). */
    public static Campo selectNumero(String nombre, String etiqueta, Function<ApiClient, List<Opcion>> opciones) {
        return nuevo(nombre, etiqueta, Tipo.SELECT_NUMERO, opciones);
    }

    /**
     * Desplegable numerico cuyas opciones dependen del valor elegido en otro campo
     * (por ejemplo, las marcas segun el tipo de vehiculo).
     */
    public static Campo selectSegun(String nombre, String etiqueta, String dependeDe,
                                    BiFunction<ApiClient, String, List<Opcion>> opcionesSegun) {
        return new Campo(nombre, etiqueta, Tipo.SELECT_NUMERO, false, false, false, null, null, null, dependeDe, opcionesSegun);
    }

    private static Campo nuevo(String n, String e, Tipo t, Function<ApiClient, List<Opcion>> o) {
        return new Campo(n, e, t, false, false, false, null, o, null, null, null);
    }

    /** Obligatorio. */
    public Campo req() {
        return new Campo(nombre, etiqueta, tipo, true, bloqueado, soloCrear, origen, opciones, vacio, dependeDe, opcionesSegun);
    }

    /** No se puede modificar al editar (es la clave del registro). */
    public Campo fijo() {
        return new Campo(nombre, etiqueta, tipo, requerido, true, soloCrear, origen, opciones, vacio, dependeDe, opcionesSegun);
    }

    /** Solo se informa al crear; al editar se ignora. */
    public Campo soloCrearA() {
        //return new Campo(nombre, etiqueta, tipo, requerido, true, soloCrear, origen, opciones, vacio);
        return new Campo(nombre, etiqueta, tipo, requerido, bloqueado, true, origen, opciones, vacio, dependeDe, opcionesSegun);
    }

    /** Ruta del dato dentro del JSON cuando no coincide con el nombre (por ejemplo "zona.letra"). */
    public Campo desde(String ruta) {
        return new Campo(nombre, etiqueta, tipo, requerido, bloqueado, soloCrear, ruta, opciones, vacio, dependeDe, opcionesSegun);
    }

    /** Texto de la opcion "ninguno" de un desplegable opcional. */
    public Campo conVacio(String texto) {
        return new Campo(nombre, etiqueta, tipo, requerido, bloqueado, soloCrear, origen, opciones, texto, dependeDe, opcionesSegun);
    }

    public boolean esSelect() {
        return tipo == Tipo.SELECT || tipo == Tipo.SELECT_NUMERO;
    }

    public boolean esCheck() {
        return tipo == Tipo.CHECK;
    }

    public boolean esInput() {
        return !esSelect() && !esCheck();
    }

    public String htmlType() {
        return switch (tipo) {
            case NUMERO, DECIMAL -> "number";
            case CLAVE -> "password";
            case MAIL -> "email";
            default -> "text";
        };
    }

    /** Las contrasenas y el usuario no son obligatorios al editar. */
    public boolean obligatorio(boolean editando) {
        return requerido && !(editando && (tipo == Tipo.CLAVE || soloCrear));
    }
}
