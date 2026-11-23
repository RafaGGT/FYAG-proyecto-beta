package com.inacap.fyagobras.domain.costo.dto;

import java.math.BigDecimal;

// DTO RentabilidadDTO: transporta datos entre el dominio y la API.
public record RentabilidadDTO(
        Long obraId,
        BigDecimal presupuestoAprobado,
        BigDecimal costoMateriales,
        BigDecimal costoManoObra,
        BigDecimal costoTotal,
        BigDecimal diferencia
) {
}
