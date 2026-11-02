package com.inacap.fyagobras.domain.user.validation.update;

import com.inacap.fyagobras.domain.user.dto.ModificarUsuarioDTO;
import com.inacap.fyagobras.domain.user.repository.UsuarioRepository;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ValidaActualizacionUsuario implements ValidarActualizarUsuario {

    @Autowired
    private UsuarioRepository repository;

    @Override
    public void validate(String username, ModificarUsuarioDTO data) {
        if (data.email() != null) {
            repository.findByEmail(data.email()).ifPresent(u -> {
                if (!u.getUsername().equals(username)) {
                    throw new ConflictoNegocioException("Este email ya esta en uso");
                }
            });
        }
    }
}