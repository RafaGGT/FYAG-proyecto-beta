package com.inacap.fyagobras.domain.user.validation.update;


import com.inacap.fyagobras.domain.user.dto.ModificarUsuarioDTO;

public interface ValidarActualizarUsuario {
    void validate(String username, ModificarUsuarioDTO data);
}