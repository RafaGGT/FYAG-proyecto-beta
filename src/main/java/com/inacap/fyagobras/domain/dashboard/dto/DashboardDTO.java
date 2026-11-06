package com.inacap.fyagobras.domain.dashboard.dto;

import java.math.BigDecimal;

public record DashboardDTO(
        BigDecimal ventasDelMes,
        long presupuestosAprobados,
        long presupuestosPendientes,
        long obrasEnProceso,
        BigDecimal cuentasPorCobrar
) {
}