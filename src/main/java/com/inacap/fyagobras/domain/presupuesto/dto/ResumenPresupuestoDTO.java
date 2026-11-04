package com.inacap.fyagobras.domain.presupuesto.dto;

import com.inacap.fyagobras.domain.presupuesto.Presupuesto;

import java.time.LocalDate;

public record ResumenPresupuestoDTO(
        Long id, String numeroPresupuesto, String clienteRazonSocial, LocalDate fechaAprobacion
) {
    public ResumenPresupuestoDTO(Presupuesto p) {
        this(p.getId(), p.getNumeroPresupuesto(), p.getCliente().getRazonSocial(), p.getFechaAprobacion());
    }
}