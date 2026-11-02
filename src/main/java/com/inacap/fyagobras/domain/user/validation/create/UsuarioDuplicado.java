package com.inacap.fyagobras.domain.user.validation.create;

import com.inacap.fyagobras.domain.user.dto.RegistrarUsuarioDTO;
import com.inacap.fyagobras.domain.user.repository.UsuarioRepository;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UsuarioDuplicado implements ValidarCrearUsuario {
    @Autowired
    private UsuarioRepository repository;

    @Override
    public void validate(RegistrarUsuarioDTO data) {
        if (repository.findByUsername(data.username()).isPresent()) {
            throw new ConflictoNegocioException("Este usuario ya existe.");
        }
        if (repository.findByEmail(data.email()).isPresent()) {
            throw new ConflictoNegocioException("Este email ya existe.");
        }
        if (repository.findByRut(data.rut()).isPresent()) {
            throw new ConflictoNegocioException("Este RUT ya existe.");
        }
    }
}