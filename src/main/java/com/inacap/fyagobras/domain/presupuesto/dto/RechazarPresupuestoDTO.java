package com.inacap.fyagobras.domain.presupuesto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// DTO RechazarPresupuestoDTO: transporta datos entre el dominio y la API.
public record RechazarPresupuestoDTO(
        @NotBlank(message = "El motivo del rechazo es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo
) {
}
