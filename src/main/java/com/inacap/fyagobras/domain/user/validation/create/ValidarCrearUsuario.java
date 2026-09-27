package com.inacap.fyagobras.domain.user.validation.create;


import com.inacap.fyagobras.domain.user.dto.RegistrarUsuarioDTO;

public interface ValidarCrearUsuario {
    void validate(RegistrarUsuarioDTO data);
}