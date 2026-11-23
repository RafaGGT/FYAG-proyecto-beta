package com.inacap.fyagobras.domain.factura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// DTO AnularPagoDTO: transporta datos entre el dominio y la API.
public record AnularPagoDTO(
        @NotBlank(message = "El motivo de la anulación es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo
) {
}
