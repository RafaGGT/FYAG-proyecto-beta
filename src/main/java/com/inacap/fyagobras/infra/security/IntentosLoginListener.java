package com.inacap.fyagobras.infra.security;

import com.inacap.fyagobras.domain.user.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;

/**
 * Escucha los eventos de autenticación de Spring Security para actualizar el
 * contador de intentos de inicio de sesión de cada usuario.
 */
@Component
public class IntentosLoginListener {

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Registra un intento fallido por credenciales incorrectas.
     *
     * @param evento evento de autenticación fallida emitido por Spring Security
     */
    @EventListener
    public void alFallar(AuthenticationFailureBadCredentialsEvent evento) {
        usuarioService.registrarFalloLogin(evento.getAuthentication().getName());
    }

    /**
     * Registra una autenticación exitosa y reinicia el contador de fallos.
     *
     * @param evento evento de autenticación exitosa emitido por Spring Security
     */
    @EventListener
    public void alAutenticar(AuthenticationSuccessEvent evento) {
        usuarioService.registrarExitoLogin(evento.getAuthentication().getName());
    }

    // Intento de ingreso contra una cuenta bloqueada por intentos fallidos
    @EventListener
    public void alCuentaBloqueada(AuthenticationFailureLockedEvent evento) {
        usuarioService.registrarIntentoRechazado(evento.getAuthentication().getName(), "LOGIN_CUENTA_BLOQUEADA");
    }

    // Intento de ingreso contra una cuenta desactivada
    @EventListener
    public void alCuentaDesactivada(AuthenticationFailureDisabledEvent evento) {
        usuarioService.registrarIntentoRechazado(evento.getAuthentication().getName(), "LOGIN_CUENTA_DESACTIVADA");
    }
}