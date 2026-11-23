package com.inacap.fyagobras.domain.seguimiento.dto;

import com.inacap.fyagobras.domain.seguimiento.EstadoPaso;

import java.time.LocalDate;

// DTO PasoSeguimientoDTO: transporta datos entre el dominio y la API.
public record PasoSeguimientoDTO(
        String titulo,
        String detalle,
        String responsable,
        LocalDate fecha,
        EstadoPaso estado
) {
}
