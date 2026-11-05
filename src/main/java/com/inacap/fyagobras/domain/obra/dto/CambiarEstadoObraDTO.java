package com.inacap.fyagobras.domain.obra.dto;

import com.inacap.fyagobras.domain.obra.EstadoObra;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambiarEstadoObraDTO(
        @NotNull(message = "El estado de destino es obligatorio")
        EstadoObra estado,

        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo
) {
}