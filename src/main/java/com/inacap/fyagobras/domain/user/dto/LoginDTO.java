package com.inacap.fyagobras.domain.user.dto;

import jakarta.validation.constraints.NotNull;

public record LoginDTO (
        @NotNull(message = "El nombre de usuario es obligatorio")
        String username,
        @NotNull(message = "La contraseña es obligatoria")
        String password
) {
}