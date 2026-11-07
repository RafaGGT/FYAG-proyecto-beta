package com.inacap.fyagobras.domain.audit;

import com.inacap.fyagobras.domain.user.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);
    private static final int MAX_INTENTOS = 3;

    @Autowired private MongoTemplate mongo;
    @Autowired private ApplicationEventPublisher publisher;

    /** Los módulos llaman a este método. El guardado real ocurre solo si la transacción se confirma. */
    public void registrar(Usuario actor, String entidad, Long entidadId, String accion,
                          String motivo, Map<String, Object> anterior, Map<String, Object> nuevo) {
        // Publica el evento para no guardar auditoría hasta confirmar la transacción.
        publisher.publishEvent(new LogAuditoria(
                actor.getId(), actor.getRole().name(), entidad, entidadId,
                accion, motivo, anterior, nuevo));
    }

    // fallbackExecution = true: también funciona cuando no hay transacción (ej: login)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void guardar(LogAuditoria registro) {
        // Reintenta el guardado unas pocas veces para tolerar fallos transitorios.
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                // Solo se inserta el registro; la auditoría es inmutable.
                mongo.insert(registro);   // solo insert: nunca update ni delete
                return;
            } catch (Exception e) {
                log.warn("Auditoría: intento {}/{} falló: {}", intento, MAX_INTENTOS, e.getMessage());
            }
        }
        log.error("Auditoría NO guardada tras {} intentos (revisar manualmente): {} {} id={}",
                MAX_INTENTOS, registro.getAccion(), registro.getEntidad(), registro.getEntidadId());
    }
}