package com.inacap.fyagobras.domain.user.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record ModificarUsuarioDTO(

        @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
        String nombre,

        @Size(max = 100, message = "El apellido no debe exceder 100 caracteres")
        String apellido,

        @Email(message = "El correo electrónico no es válido")
        @Size(max = 150, message = "El correo electrónico no debe exceder 150 caracteres")
        String email,

        @Size(max = 20, message = "El teléfono no debe exceder 20 caracteres")
        String telefono
) {
}