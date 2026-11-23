package com.inacap.fyagobras.domain.obra.dto;

import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.obra.Obra;

import java.time.LocalDate;
import java.util.List;

// DTO DetallesObraDTO: transporta datos entre el dominio y la API.
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
        String tecnicoNombre,
        // Servicios/materiales cotizados por Finanzas: solo la descripción, nunca el monto
        List<String> itemsPresupuesto
) {
    // Ejecuta la operacion DetallesObraDTO.
    public DetallesObraDTO(Obra o) {
        this(o.getId(), o.getNombre(), o.getDescripcion(), o.getEstado(),
                o.getFechaInicio(), o.getFechaTermino(), o.getMotivoSuspension(),
                o.getPresupuesto().getId(),
                o.getPresupuesto().getCliente().getRazonSocial(),
                o.getTipoServicio().getId(), o.getTipoServicio().getNombre(),
                o.getTecnico() == null ? null : o.getTecnico().getId(),
                o.getTecnico() == null ? null : o.getTecnico().getNombre() + " " + o.getTecnico().getApellido(),
                o.getPresupuesto().getItems().stream().map(i -> i.getDescripcion()).toList());
    }
}
