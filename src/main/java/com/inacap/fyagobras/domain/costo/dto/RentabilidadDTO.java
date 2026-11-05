package com.inacap.fyagobras.domain.costo.dto;

import java.math.BigDecimal;

public record RentabilidadDTO(
        Long obraId,
        BigDecimal presupuestoAprobado,
        BigDecimal costoMateriales,
        BigDecimal costoManoObra,
        BigDecimal costoTotal,
        BigDecimal diferencia
) {
}