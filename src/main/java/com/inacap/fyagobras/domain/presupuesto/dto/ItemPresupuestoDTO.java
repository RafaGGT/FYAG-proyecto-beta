package com.inacap.fyagobras.domain.presupuesto.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemPresupuestoDTO(
        @NotBlank(message = "La descripción del ítem es obligatoria")
        @Size(max = 300, message = "La descripción no debe exceder 300 caracteres")
        String descripcion,

        @NotNull(message = "El monto del ítem es obligatorio")
        @Positive(message = "El monto debe ser mayor que cero")
        @Digits(integer = 12, fraction = 2, message = "Monto fuera de rango")
        BigDecimal monto
) {
}