package com.inacap.fyagobras.domain.costo.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record RegistrarCostosDTO(
        @NotNull(message = "El costo de materiales es obligatorio")
        @PositiveOrZero(message = "Los materiales no pueden ser negativos")
        @Digits(integer = 12, fraction = 2, message = "Formato de monto inválido")
        BigDecimal materiales,

        @NotNull(message = "Las horas técnicas son obligatorias")
        @PositiveOrZero(message = "Las horas no pueden ser negativas")
        @Digits(integer = 6, fraction = 2, message = "Formato de horas inválido")
        BigDecimal horasTecnicas,

        @NotNull(message = "El valor hora es obligatorio")
        @PositiveOrZero(message = "El valor hora no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "Formato de monto inválido")
        BigDecimal valorHora
) {
}