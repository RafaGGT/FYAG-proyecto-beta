package com.inacap.fyagobras.domain.audit;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Document(collection = "log_auditoria")
// Documento de MongoDB que representa un evento histórico de auditoría.
public class LogAuditoria {

    // Identificador generado por MongoDB para localizar el registro.
    @Id
    private String id;

    // Información del usuario que realizó la acción auditada.
    @Field("usuario_id")      private Long usuarioId;
    @Field("usuario_rol")     private String usuarioRol;
    // Entidad del sistema y registro concreto que fueron afectados.
    private String entidad;
    @Field("entidad_id")      private Long entidadId;
    // Acción ejecutada y explicación opcional del motivo.
    private String accion;
    private String motivo;
    // Estado anterior y nuevo de los datos relevantes de la operación.
    @Field("valor_anterior")  private Map<String, Object> valorAnterior;
    @Field("valor_nuevo")     private Map<String, Object> valorNuevo;
    // Momento exacto en que se creó el evento de auditoría.
    private Instant fecha;

    // Ejecuta la operacion LogAuditoria.
    public LogAuditoria(Long usuarioId, String usuarioRol, String entidad, Long entidadId,
                        String accion, String motivo,
                        Map<String, Object> valorAnterior, Map<String, Object> valorNuevo) {
        // Copia los datos principales del usuario y de la entidad afectada.
        this.usuarioId = usuarioId;
        this.usuarioRol = usuarioRol;
        this.entidad = entidad;
        this.entidadId = entidadId;
        // Guarda la acción, el motivo y los valores asociados.
        this.accion = accion;
        this.motivo = motivo;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
        // Registra automáticamente el instante de creación.
        this.fecha = Instant.now();
    }

    // Solo getters: el documento nunca se modifica
    // Obtiene el identificador MongoDB del documento.
    public String getId() { return id; }
    // Obtiene el identificador del usuario que realizó la acción.
    public Long getUsuarioId() { return usuarioId; }
    // Obtiene el rol del usuario en el momento de la acción.
    public String getUsuarioRol() { return usuarioRol; }
    // Obtiene el nombre de la entidad afectada.
    public String getEntidad() { return entidad; }
    // Obtiene el identificador del registro afectado.
    public Long getEntidadId() { return entidadId; }
    // Obtiene el nombre de la acción realizada.
    public String getAccion() { return accion; }
    // Obtiene el motivo registrado para la acción.
    public String getMotivo() { return motivo; }
    // Obtiene los valores que existían antes del cambio.
    public Map<String, Object> getValorAnterior() { return valorAnterior; }
    // Obtiene los valores resultantes después del cambio.
    public Map<String, Object> getValorNuevo() { return valorNuevo; }
    // Obtiene la fecha y hora del evento.
    public Instant getFecha() { return fecha; }
}
