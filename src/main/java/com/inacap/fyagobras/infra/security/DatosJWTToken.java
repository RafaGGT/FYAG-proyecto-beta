package com.inacap.fyagobras.infra.security;

/**
 * Datos de autenticación devueltos al cliente después de iniciar sesión o
 * actualizar los tokens.
 *
 * @param token token JWT de acceso para las solicitudes protegidas
 * @param refreshToken token utilizado para renovar la autenticación
 */
public record DatosJWTToken(String token, String refreshToken) {
}