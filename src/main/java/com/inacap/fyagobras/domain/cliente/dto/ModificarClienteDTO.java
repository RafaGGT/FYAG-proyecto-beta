package com.inacap.fyagobras.domain.cliente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record ModificarClienteDTO(
        @Size(min = 1, max = 150, message = "La razón social debe tener entre 1 y 150 caracteres")
        String razonSocial,

        @Size(max = 200, message = "La dirección no debe exceder 200 caracteres")
        String direccion,

        @Email(message = "El correo electrónico no es válido")
        @Size(max = 150, message = "El correo electrónico no debe exceder 150 caracteres")
        String email,

        @Size(max = 20, message = "El teléfono no debe exceder 20 caracteres")
        String telefono
) {
}