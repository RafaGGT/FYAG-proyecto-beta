package com.inacap.fyagobras.domain.obra.dto;

import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.obra.Obra;

import java.time.LocalDate;

public record DetallesObraDTO(
        Long id,
        String nombre,
        String descripcion,
        EstadoObra estado,
        LocalDate fechaInicio,
        LocalDate fechaTermino,
        String motivoSuspension,
        Long presupuestoId,
        String clienteRazonSocial,
        Long tipoServicioId,
        String tipoServicioNombre,
        Long tecnicoId,
        String tecnicoNombre
) {
    public DetallesObraDTO(Obra o) {
        this(o.getId(), o.getNombre(), o.getDescripcion(), o.getEstado(),
                o.getFechaInicio(), o.getFechaTermino(), o.getMotivoSuspension(),
                o.getPresupuesto().getId(),
                o.getPresupuesto().getCliente().getRazonSocial(),
                o.getTipoServicio().getId(), o.getTipoServicio().getNombre(),
                o.getTecnico() == null ? null : o.getTecnico().getId(),
                o.getTecnico() == null ? null : o.getTecnico().getNombre() + " " + o.getTecnico().getApellido());
    }
}
