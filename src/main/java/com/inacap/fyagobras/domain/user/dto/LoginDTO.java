package com.inacap.fyagobras.domain.user.dto;

import jakarta.validation.constraints.NotNull;

// DTO LoginDTO: transporta datos entre el dominio y la API.
public record LoginDTO (
        @NotNull(message = "El nombre de usuario es obligatorio")
        String username,
        @NotNull(message = "La contraseña es obligatoria")
        String password
) {
}
