package com.inacap.fyagobras.domain.user.validation.update;


import com.inacap.fyagobras.domain.user.dto.ModificarUsuarioDTO;

// Validador ValidarActualizarUsuario: aplica una regla del dominio.
public interface ValidarActualizarUsuario {
    // Ejecuta la operacion validate.
    void validate(String username, ModificarUsuarioDTO data);
}
