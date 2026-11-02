package com.inacap.fyagobras.domain.audit;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Document(collection = "log_auditoria")
public class LogAuditoria {

    @Id
    private String id;

    @Field("usuario_id")      private Long usuarioId;
    @Field("usuario_rol")     private String usuarioRol;
    private String entidad;
    @Field("entidad_id")      private Long entidadId;
    private String accion;
    private String motivo;
    @Field("valor_anterior")  private Map<String, Object> valorAnterior;
    @Field("valor_nuevo")     private Map<String, Object> valorNuevo;
    private Instant fecha;

    public LogAuditoria(Long usuarioId, String usuarioRol, String entidad, Long entidadId,
                        String accion, String motivo,
                        Map<String, Object> valorAnterior, Map<String, Object> valorNuevo) {
        this.usuarioId = usuarioId;
        this.usuarioRol = usuarioRol;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.accion = accion;
        this.motivo = motivo;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
        this.fecha = Instant.now();
    }

    // Solo getters: el documento nunca se modifica
    public String getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getUsuarioRol() { return usuarioRol; }
    public String getEntidad() { return entidad; }
    public Long getEntidadId() { return entidadId; }
    public String getAccion() { return accion; }
    public String getMotivo() { return motivo; }
    public Map<String, Object> getValorAnterior() { return valorAnterior; }
    public Map<String, Object> getValorNuevo() { return valorNuevo; }
    public Instant getFecha() { return fecha; }
}