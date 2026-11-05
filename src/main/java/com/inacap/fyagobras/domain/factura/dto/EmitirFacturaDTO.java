package com.inacap.fyagobras.domain.factura.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmitirFacturaDTO(
        @NotBlank(message = "El número de factura es obligatorio")
        @Size(max = 30, message = "El número de factura no puede superar 30 caracteres")
        String numeroFactura,

        @NotNull(message = "Debe seleccionar una obra")
        Long obraId,

        @NotNull(message = "La fecha de emisión es obligatoria")
        LocalDate fechaEmision,

        @NotNull(message = "La fecha de vencimiento es obligatoria")
        LocalDate fechaVencimiento,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor que cero")
        @Digits(integer = 12, fraction = 2, message = "Formato de monto inválido")
        BigDecimal monto
) {
}