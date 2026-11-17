package com.inacap.fyagobras.domain.factura.dto;

import java.math.BigDecimal;

public record ObraFacturableDTO(
        Long obraId,
        String obraNombre,
        String clienteRazonSocial,
        BigDecimal montoPresupuesto,
        BigDecimal totalFacturado,
        BigDecimal saldoPorFacturar
) {
}