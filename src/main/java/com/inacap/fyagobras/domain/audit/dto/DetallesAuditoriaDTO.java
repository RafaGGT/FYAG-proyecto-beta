package com.inacap.fyagobras.domain.audit.dto;

import com.inacap.fyagobras.domain.audit.LogAuditoria;

import java.time.Instant;
import java.util.Map;

public record DetallesAuditoriaDTO(
        String id,
        Long usuarioId,
        String usuarioRol,
        String entidad,
        Long entidadId,
        String accion,
        String motivo,
        Map<String, Object> valorAnterior,
        Map<String, Object> valorNuevo,
        Instant fecha) {

    public DetallesAuditoriaDTO(LogAuditoria l) {
        this(l.getId(), l.getUsuarioId(), l.getUsuarioRol(), l.getEntidad(), l.getEntidadId(),
                l.getAccion(), l.getMotivo(), l.getValorAnterior(), l.getValorNuevo(), l.getFecha());
    }
}