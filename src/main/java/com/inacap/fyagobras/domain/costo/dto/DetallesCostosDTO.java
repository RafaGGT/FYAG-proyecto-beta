package com.inacap.fyagobras.domain.costo.dto;

import com.inacap.fyagobras.domain.costo.CostoReal;

import java.math.BigDecimal;

public record DetallesCostosDTO(
        Long obraId,
        BigDecimal materiales,
        BigDecimal horasTecnicas,
        BigDecimal valorHora
) {
    public DetallesCostosDTO(CostoReal c) {
        this(c.getObra().getId(), c.getMateriales(), c.getHorasTecnicas(), c.getValorHora());
    }
}