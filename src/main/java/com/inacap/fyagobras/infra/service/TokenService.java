package com.inacap.fyagobras.infra.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.inacap.fyagobras.infra.exception.TokenInvalidoException;
import com.inacap.fyagobras.domain.user.Usuario;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {
    @Value("${api.security.token.secret}")
    private String secret;

    // Se ejecuta una sola vez, al arrancar el servicio -- si el secreto es
    // débil, el contexto de Spring NO termina de levantar (falla rápido
    // y claro, en vez de arrancar "bien" con un hueco de seguridad silencioso).
    @PostConstruct
    public void validarSecret() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET es demasiado corto o no fue configurado (mínimo 32 caracteres). " +
                            "Define la variable de entorno JWT_SECRET con un valor largo y aleatorio."
            );
        }
    }

    public String generarToken(Usuario usuario) {
        try {
            var algoritmo = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("Api Bodega") //
                    .withSubject(usuario.getUsername())
                    .withClaim("type", "access") // marca este token como de acceso
                    .withClaim("role", usuario.getRole().name()) // para que otros microservicios (inventory-service) sepan el rol sin llamarte
                    .withExpiresAt(fechaExpiracionAcceso())
                    .sign(algoritmo);
        } catch (JWTCreationException exception){
            throw new RuntimeException("error al generar el token JWT", exception);
        }
    }

    public String generarRefreshToken(Usuario usuario) {
        try {
            var algoritmo = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("Api Bodega")
                    .withSubject(usuario.getId().toString())
                    .withClaim("type", "refresh") // marca este token como de refresco
                    .withExpiresAt(fechaExpiracionRefresh())
                    .sign(algoritmo);
        } catch (JWTCreationException exception){
            throw new RuntimeException("Error al generar el refresh token JWT", exception);
        }
    }

    // Verifica firma + expiración + issuer, y además que el claim "type" coincida
// con lo que el endpoint espera recibir (access o refresh, nunca el otro)
    public DecodedJWT verificarToken(String tokenJWT, String tipoEsperado) {
        try {
            var algoritmo = Algorithm.HMAC256(secret);
            return JWT.require(algoritmo)
                    .withIssuer("Api Bodega")
                    .withClaim("type", tipoEsperado) // rechaza el token si el claim no coincide
                    .build()
                    .verify(tokenJWT);
        } catch (JWTVerificationException exception) {
            throw new TokenInvalidoException("Token JWT inválido o expirado", exception);
        }
    }

    public String getSubject(String tokenJWT) {
        return verificarToken(tokenJWT, "access").getSubject();
    }

    private Instant fechaExpiracionAcceso() {
        return Instant.now().plus(30, ChronoUnit.MINUTES);
    }

    private Instant fechaExpiracionRefresh() {
        return Instant.now().plus(7, ChronoUnit.DAYS);
    }

}