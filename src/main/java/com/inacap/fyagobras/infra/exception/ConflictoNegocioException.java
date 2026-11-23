package com.inacap.fyagobras.infra.exception;

/**
 * Indica que una operación es válida a nivel técnico, pero incumple una regla
 * o condición del negocio.
 */
// Representa errores producidos por reglas de negocio invalidas.
public class ConflictoNegocioException extends RuntimeException {

    /**
     * Crea una excepción con el motivo del conflicto.
     *
     * @param msg descripción de la regla de negocio incumplida
     */
    public ConflictoNegocioException(String msg) { super(msg); }
}