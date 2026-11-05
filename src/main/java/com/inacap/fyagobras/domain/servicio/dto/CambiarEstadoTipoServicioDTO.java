package com.inacap.fyagobras.domain.servicio.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoTipoServicioDTO(
        @NotNull(message = "Debe indicar si el tipo queda activo")
        Boolean activo
) {
}