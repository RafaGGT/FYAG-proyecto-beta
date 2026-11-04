package com.inacap.fyagobras.domain.presupuesto.dto;

import com.inacap.fyagobras.domain.presupuesto.EstadoPresupuesto;
import com.inacap.fyagobras.domain.presupuesto.Presupuesto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ListaPresupuestoDTO(
        Long id, String numeroPresupuesto, String clienteRazonSocial,
        EstadoPresupuesto estado, LocalDate fechaCreacion, BigDecimal montoTotal
) {
    public ListaPresupuestoDTO(Presupuesto p) {
        this(p.getId(), p.getNumeroPresupuesto(), p.getCliente().getRazonSocial(),
                p.getEstado(), p.getFechaCreacion(), p.getMontoTotal());
    }
}