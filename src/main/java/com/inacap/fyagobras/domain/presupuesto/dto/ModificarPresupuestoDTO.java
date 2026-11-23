package com.inacap.fyagobras.domain.presupuesto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// DTO ModificarPresupuestoDTO: transporta datos entre el dominio y la API.
public record ModificarPresupuestoDTO(
        @NotEmpty(message = "Debe incluir al menos un ítem")
        List<@Valid ItemPresupuestoDTO> items
) {
}
