package com.inacap.fyagobras.infra.exception;

/**
 * Indica que no se encontró el recurso solicitado.
 */
// Esta excepción representa una búsqueda que no encontró su recurso.
// Al lanzar esta clase, el GestorErrores convierte el error en una respuesta HTTP 404.
public class RecursoNoEncontradoException extends RuntimeException {

    /**
     * Crea una excepción con el detalle del recurso ausente.
     *
     * @param msg descripción del recurso que no fue encontrado
     */
    public RecursoNoEncontradoException(String msg) {
        // RuntimeException almacena el texto recibido.
        // Ese texto luego puede mostrarse al usuario para indicar qué recurso no existe.
        super(msg);
    }
}