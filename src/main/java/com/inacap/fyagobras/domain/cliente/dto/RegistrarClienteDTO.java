package com.inacap.fyagobras.domain.cliente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrarClienteDTO(
        @NotBlank(message = "El RUT es obligatorio")
        @Pattern(regexp = "^\\d{7,8}-[\\dkK]$", message = "Formato de RUT inválido (ej: 12345678-5)")
        String rut,

        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 150, message = "La razón social no debe exceder 150 caracteres")
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