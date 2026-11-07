package com.inacap.fyagobras.infra.exception;

/**
 * Indica que un token de autenticación no es válido, está expirado o no puede
 * asociarse con un usuario habilitado.
 */
public class TokenInvalidoException extends RuntimeException {

    /**
     * Crea una excepción de token inválido.
     *
     * @param msg descripción del motivo del rechazo
     * @param causa excepción original que produjo el rechazo, si existe
     */
    public TokenInvalidoException(String msg, Throwable causa) { super(msg, causa); }
}