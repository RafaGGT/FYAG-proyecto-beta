package com.inacap.fyagobras.domain.audit;

import com.inacap.fyagobras.domain.audit.dto.DetallesAuditoriaDTO;
import com.inacap.fyagobras.domain.audit.repository.AuditoriaRepository;
import com.inacap.fyagobras.domain.user.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.time.ZoneId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Map;

@Service
// Coordina la creación, persistencia y consulta de registros de auditoría.
public class AuditoriaService {

    // Herramientas usadas para registrar errores y limitar los reintentos.
    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);
    private static final int MAX_INTENTOS = 3;
    // Zona horaria usada para interpretar los filtros por fecha.
    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    // Dependencias necesarias para MongoDB, eventos y consultas.
    @Autowired private MongoTemplate mongo;
    @Autowired private ApplicationEventPublisher publisher;
    @Autowired private AuditoriaRepository auditoriaRepository;

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
    // Ejecuta la operacion guardar.
    public void guardar(LogAuditoria registro) {
        // Reintenta el guardado unas pocas veces para tolerar fallos transitorios.
        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {
            try {
                // Solo se inserta el registro; la auditoría es inmutable.
                mongo.insert(registro);   // solo insert: nunca update ni delete
                return;
            } catch (Exception e) {
                // Registra el fallo y continúa con el siguiente intento.
                log.warn("Auditoría: intento {}/{} falló: {}", intento, MAX_INTENTOS, e.getMessage());
            }
        }
        // Si todos los intentos fallan, deja una advertencia crítica para revisión manual.
        log.error("Auditoría NO guardada tras {} intentos (revisar manualmente): {} {} id={}",
                MAX_INTENTOS, registro.getAccion(), registro.getEntidad(), registro.getEntidadId());
    }

    // Ejecuta la operacion listar.
    public Page<DetallesAuditoriaDTO> listar(Pageable paginacion) {
        // Consulta todos los registros y convierte cada uno a su DTO público.
        return auditoriaRepository.findAll(paginacion).map(DetallesAuditoriaDTO::new);
    }

    // Ejecuta la operacion listarPorUsuario.
    public Page<DetallesAuditoriaDTO> listarPorUsuario(Long usuarioId, Pageable paginacion) {
        // Filtra los eventos realizados por el usuario indicado.
        return auditoriaRepository.findByUsuarioId(usuarioId, paginacion).map(DetallesAuditoriaDTO::new);
    }

    // Ejecuta la operacion listarPorFecha.
    public Page<DetallesAuditoriaDTO> listarPorFecha(LocalDate desde, LocalDate hasta, Pageable paginacion) {
        // Incluye el día "hasta" completo
        // Convierte el inicio del primer día a UTC instant.
        var inicio = desde.atStartOfDay(ZONA).toInstant();
        // Avanza un día para incluir todas las horas del día final.
        var fin = hasta.plusDays(1).atStartOfDay(ZONA).toInstant();
        // Consulta MongoDB dentro del rango y devuelve los registros como DTO.
        return auditoriaRepository.findByFechaBetween(inicio, fin, paginacion).map(DetallesAuditoriaDTO::new);
    }
}
