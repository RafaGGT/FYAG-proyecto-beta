package com.inacap.fyagobras.domain.incidencia.dto;

import com.inacap.fyagobras.domain.incidencia.IncidenciaObra;
import com.inacap.fyagobras.domain.incidencia.TipoIncidencia;

import java.time.Instant;

public record DetallesIncidenciaDTO(
        Long id,
        TipoIncidencia tipo,
        String texto,
        Long autorId,
        String autorNombre,
        Instant fecha
) {
    public DetallesIncidenciaDTO(IncidenciaObra i) {
        this(i.getId(), i.getTipo(), i.getTexto(), i.getAutor().getId(),
                i.getAutor().getNombre() + " " + i.getAutor().getApellido(), i.getFecha());
    }
}