package com.inacap.fyagobras.domain.seguimiento.dto;

import java.util.List;

// DTO SeguimientoDTO: transporta datos entre el dominio y la API.
public record SeguimientoDTO(
        String resumen,
        List<PasoSeguimientoDTO> pasos
) {
}
