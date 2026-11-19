package com.inacap.fyagobras.domain.seguimiento.dto;

import java.util.List;

public record SeguimientoDTO(
        String resumen,
        List<PasoSeguimientoDTO> pasos
) {
}
