package com.inacap.fyagobras.domain.dashboard.dto;

import java.math.BigDecimal;

// Representa los indicadores resumidos que consume el dashboard.
public record DashboardDTO(
        BigDecimal ventasDelMes,
        long presupuestosAprobados,
        long presupuestosPendientes,
        long obrasEnProceso,
        BigDecimal cuentasPorCobrar
) {
}
