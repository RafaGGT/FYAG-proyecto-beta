package com.inacap.fyagobras.infra.exception;

/**
 * Indica que no se encontró el recurso solicitado.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    /**
     * Crea una excepción con el detalle del recurso ausente.
     *
     * @param msg descripción del recurso que no fue encontrado
     */
    public RecursoNoEncontradoException(String msg) { super(msg); }
}