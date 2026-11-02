package com.inacap.fyagobras.infra.security;

import com.inacap.fyagobras.domain.user.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

// Escucha los eventos de Spring Security para contar los intentos de login
@Component
public class IntentosLoginListener {

    @Autowired
    private UsuarioService usuarioService;

    // Clave incorrecta: suma un intento fallido (al llegar al máximo se bloquea la cuenta)
    @EventListener
    public void alFallar(AuthenticationFailureBadCredentialsEvent evento) {
        usuarioService.registrarFalloLogin(evento.getAuthentication().getName());
    }

    // Autenticación exitosa: reinicia el contador de intentos fallidos
    @EventListener
    public void alAutenticar(AuthenticationSuccessEvent evento) {
        usuarioService.registrarExitoLogin(evento.getAuthentication().getName());
    }
}