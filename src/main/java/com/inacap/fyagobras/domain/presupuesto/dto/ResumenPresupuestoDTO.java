package com.inacap.fyagobras.domain.presupuesto.dto;

import com.inacap.fyagobras.domain.presupuesto.Presupuesto;

import java.time.LocalDate;

// DTO ResumenPresupuestoDTO: transporta datos entre el dominio y la API.
public record ResumenPresupuestoDTO(
        Long id, String numeroPresupuesto, String clienteRazonSocial, LocalDate fechaAprobacion
) {
    // Ejecuta la operacion ResumenPresupuestoDTO.
    public ResumenPresupuestoDTO(Presupuesto p) {
        this(p.getId(), p.getNumeroPresupuesto(), p.getCliente().getRazonSocial(), p.getFechaAprobacion());
    }
}
