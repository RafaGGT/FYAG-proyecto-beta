package com.inacap.fyagobras.domain.presupuesto.dto;

import com.inacap.fyagobras.domain.presupuesto.EstadoPresupuesto;
import com.inacap.fyagobras.domain.presupuesto.Presupuesto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DetallesPresupuestoDTO(
        Long id,
        String numeroPresupuesto,
        Long clienteId,
        String clienteRazonSocial,
        EstadoPresupuesto estado,
        LocalDate fechaCreacion,
        LocalDate fechaAprobacion,
        String motivoRechazo,
        BigDecimal montoTotal,
        List<ItemPresupuestoDTO> items
) {
    public DetallesPresupuestoDTO(Presupuesto p) {
        this(p.getId(), p.getNumeroPresupuesto(), p.getCliente().getId(), p.getCliente().getRazonSocial(),
                p.getEstado(), p.getFechaCreacion(), p.getFechaAprobacion(), p.getMotivoRechazo(),
                p.getMontoTotal(),
                p.getItems().stream().map(i -> new ItemPresupuestoDTO(i.getDescripcion(), i.getMonto())).toList());
    }
}