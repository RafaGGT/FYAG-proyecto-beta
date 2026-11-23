package com.inacap.fyagobras.domain.incidencia.dto;

import com.inacap.fyagobras.domain.incidencia.TipoIncidencia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// DTO RegistrarIncidenciaDTO: transporta datos entre el dominio y la API.
public record RegistrarIncidenciaDTO(
        @NotNull(message = "Elige el tipo de novedad")
        TipoIncidencia tipo,

        @NotBlank(message = "Escribe qué ocurrió")
        @Size(max = 500, message = "Máximo 500 caracteres")
        String texto
) {
}
