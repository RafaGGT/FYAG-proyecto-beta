package com.inacap.fyagobras.domain.obra.dto;

import jakarta.validation.constraints.NotNull;

// DTO AsignarTecnicoDTO: transporta datos entre el dominio y la API.
public record AsignarTecnicoDTO(
        // Id del técnico que pasa a ser el responsable de la obra
        @NotNull(message = "Debe seleccionar un técnico")
        Long tecnicoId
) {
}
