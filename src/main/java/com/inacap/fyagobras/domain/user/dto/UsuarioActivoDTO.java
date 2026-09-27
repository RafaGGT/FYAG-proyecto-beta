package com.inacap.fyagobras.domain.user.dto;

import jakarta.validation.constraints.NotNull;

public record UsuarioActivoDTO(
        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {
}