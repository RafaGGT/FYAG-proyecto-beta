package com.inacap.fyagobras.infra.exception;

public class TokenInvalidoException extends RuntimeException {
    public TokenInvalidoException(String msg, Throwable causa) { super(msg, causa); }
}