package com.inacap.fyagobras.domain.factura.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

// DTO ClientePendienteDTO: transporta datos entre el dominio y la API.
public record ClientePendienteDTO(
        Long clienteId,
        String rut,
        String razonSocial,
        long facturasPendientes,
        BigDecimal montoAdeudado,
        LocalDate vencimientoMasAntiguo,
        long diasAtraso
) {
}
