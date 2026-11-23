package com.inacap.fyagobras.domain.costo.dto;

import com.inacap.fyagobras.domain.costo.CostoReal;
import com.inacap.fyagobras.domain.user.Role;

import java.math.BigDecimal;

// DTO DetallesCostosDTO: transporta datos entre el dominio y la API.
public record DetallesCostosDTO(
        Long obraId,
        BigDecimal materiales,
        BigDecimal horasTecnicas,
        BigDecimal valorHora
) {
    // Ejecuta la operacion DetallesCostosDTO.
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
