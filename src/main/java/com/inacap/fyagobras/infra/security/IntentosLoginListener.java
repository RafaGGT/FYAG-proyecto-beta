package com.inacap.fyagobras.infra.security;

import com.inacap.fyagobras.domain.user.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class IntentosLoginListener {

    @Autowired
    private UsuarioService usuarioService;

    @EventListener
    public void alFallar(AuthenticationFailureBadCredentialsEvent evento) {
        usuarioService.registrarFalloLogin(evento.getAuthentication().getName());
    }

    @EventListener
    public void alAutenticar(AuthenticationSuccessEvent evento) {
        usuarioService.registrarExitoLogin(evento.getAuthentication().getName());
    }
}