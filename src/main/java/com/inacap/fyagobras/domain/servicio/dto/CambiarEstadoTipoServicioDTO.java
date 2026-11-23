package com.inacap.fyagobras.domain.servicio.dto;

import jakarta.validation.constraints.NotNull;

// DTO CambiarEstadoTipoServicioDTO: transporta datos entre el dominio y la API.
public record CambiarEstadoTipoServicioDTO(
        @NotNull(message = "Debe indicar si el tipo queda activo")
        Boolean activo
) {
}
