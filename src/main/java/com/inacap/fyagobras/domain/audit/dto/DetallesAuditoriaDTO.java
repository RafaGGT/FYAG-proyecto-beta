package com.inacap.fyagobras.domain.audit.dto;

import com.inacap.fyagobras.domain.audit.LogAuditoria;

import java.time.Instant;
import java.util.Map;

// DTO que expone los datos públicos de un registro de auditoría.
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
    // Crea un DTO a partir de un documento MongoDB de auditoría, copiando sus propiedades relevantes.
    public DetallesAuditoriaDTO(LogAuditoria l) {
        // Copia los datos del documento MongoDB a las propiedades del DTO.
        this(l.getId(), l.getUsuarioId(), l.getUsuarioRol(), l.getEntidad(), l.getEntidadId(),
                l.getAccion(), l.getMotivo(), l.getValorAnterior(), l.getValorNuevo(), l.getFecha());
    }
}
