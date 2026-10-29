package com.inacap.fyagobras.domain.user.dto;

import com.inacap.fyagobras.domain.user.Role;
import jakarta.validation.constraints.*;


public record RegistrarUsuarioDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no debe exceder 100 caracteres")
        String apellido,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no es válido")
        @Size(max = 150, message = "El correo electrónico no debe exceder 150 caracteres")
        String email,

        @Size(max = 20, message = "El teléfono no debe exceder 20 caracteres")
        String telefono,

        @NotBlank(message = "El RUT es obligatorio")
        @Pattern(regexp = "^\\d{7,8}-[\\dkK]$", message = "Formato de RUT inválido (ej: 12345678-5)")
        String rut,

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 4, max = 20, message = "El username debe tener entre 4 y 20 caracteres")
        @Pattern(regexp = "^(?!(me|tecnicos)$)[a-zA-Z0-9_]+$",
                message = "El username solo puede contener letras, números y guión bajo, y no puede ser 'me' ni 'tecnicos'")
        String username,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @NotNull(message = "El rol es obligatorio")
        Role role
) {
}