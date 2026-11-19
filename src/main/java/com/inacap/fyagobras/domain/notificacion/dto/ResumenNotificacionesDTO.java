package com.inacap.fyagobras.domain.notificacion.dto;

import java.util.List;

/** Lo que necesita la campana: cuántas hay sin leer y las más recientes. */
public record ResumenNotificacionesDTO(
        long noLeidas,
        List<NotificacionDTO> items
) {
}