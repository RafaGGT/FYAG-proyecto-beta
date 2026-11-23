package com.inacap.fyagobras.domain.factura.dto;

import java.math.BigDecimal;

// DTO ObraFacturableDTO: transporta datos entre el dominio y la API.
public record ObraFacturableDTO(
        Long obraId,
        String obraNombre,
        String clienteRazonSocial,
        BigDecimal montoPresupuesto,
        BigDecimal totalFacturado,
        BigDecimal saldoPorFacturar
) {
}
