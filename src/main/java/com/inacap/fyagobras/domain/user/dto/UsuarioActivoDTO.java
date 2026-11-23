package com.inacap.fyagobras.domain.user.dto;

import jakarta.validation.constraints.NotNull;

// DTO UsuarioActivoDTO: transporta datos entre el dominio y la API.
public record UsuarioActivoDTO(
        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {
}
