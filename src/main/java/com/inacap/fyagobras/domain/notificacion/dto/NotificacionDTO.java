package com.inacap.fyagobras.domain.notificacion.dto;

import com.inacap.fyagobras.domain.notificacion.Notificacion;
import com.inacap.fyagobras.domain.notificacion.TipoNotificacion;

import java.time.Instant;

// DTO NotificacionDTO: transporta datos entre el dominio y la API.
public record NotificacionDTO(
        Long id,
        TipoNotificacion tipo,
        String mensaje,
        String tono,
        Long obraId,
        boolean leida,
        Instant fechaCreacion
) {
    // Ejecuta la operacion NotificacionDTO.
    public NotificacionDTO(Notificacion n) {
        this(n.getId(), n.getTipo(), n.getMensaje(), n.getTono(), n.getObraId(), n.isLeida(), n.getFechaCreacion());
    }
}
