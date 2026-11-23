package com.inacap.fyagobras.domain.factura.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// DTO RegistrarPagoDTO: transporta datos entre el dominio y la API.
public record RegistrarPagoDTO(
        @NotNull(message = "La fecha de pago es obligatoria")
        LocalDate fechaPago
) {
}
