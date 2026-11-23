package com.inacap.fyagobras.domain.user.validation.create;


import com.inacap.fyagobras.domain.user.dto.RegistrarUsuarioDTO;

// Validador ValidarCrearUsuario: aplica una regla del dominio.
public interface ValidarCrearUsuario {
    // Ejecuta la operacion validate.
    void validate(RegistrarUsuarioDTO data);
}
