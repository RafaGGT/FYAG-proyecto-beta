package com.inacap.fyagobras.domain.presupuesto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RegistrarPresupuestoDTO(
        @NotNull(message = "El cliente es obligatorio")
        Long clienteId,

        @NotEmpty(message = "Debe incluir al menos un ítem")
        List<@Valid ItemPresupuestoDTO> items
) {
}