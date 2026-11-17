package com.inacap.fyagobras.domain.costo.dto;

import com.inacap.fyagobras.domain.costo.CostoReal;
import com.inacap.fyagobras.domain.user.Role;

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

    // Versión según el rol: al Técnico NO se le entrega el valor hora (solo lo que él registra)
    public static DetallesCostosDTO para(CostoReal c, Role rol) {
        if (rol == Role.TECNICO) {
            return new DetallesCostosDTO(c.getObra().getId(), c.getMateriales(), c.getHorasTecnicas(), null);
        }
        return new DetallesCostosDTO(c);
    }
}