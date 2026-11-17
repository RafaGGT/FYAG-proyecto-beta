package com.inacap.fyagobras.domain.user.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record ModificarUsuarioDTO(

        @Size(max = 40, message = "El nombre no debe exceder 40 caracteres")
        String nombre,

        @Size(max = 40, message = "El apellido no debe exceder 40 caracteres")
        String apellido,

        @Email(message = "El correo electrónico no es válido")
        @Size(max = 40, message = "El correo electrónico no debe exceder 40 caracteres")
        String email,

        @Size(max = 12, message = "El teléfono no debe exceder 12 caracteres")
        String telefono
) {
}