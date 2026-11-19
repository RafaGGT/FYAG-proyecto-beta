package com.inacap.fyagobras.domain.notificacion.dto;

import com.inacap.fyagobras.domain.notificacion.Notificacion;
import com.inacap.fyagobras.domain.notificacion.TipoNotificacion;

import java.time.Instant;

public record NotificacionDTO(
        Long id,
        TipoNotificacion tipo,
        String mensaje,
        String tono,
        Long obraId,
        boolean leida,
        Instant fechaCreacion
) {
    public NotificacionDTO(Notificacion n) {
        this(n.getId(), n.getTipo(), n.getMensaje(), n.getTono(), n.getObraId(), n.isLeida(), n.getFechaCreacion());
    }
}