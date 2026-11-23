package com.inacap.fyagobras.auditoria;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.audit.LogAuditoria;
import com.inacap.fyagobras.domain.audit.repository.AuditoriaRepository;
import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * CP: Revisar log_auditoria tras las acciones del ciclo / MongoDB no disponible tras confirmar un cambio.
 * Trazabilidad: RNF-02, A08 y A09 (OWASP). Tipo: integración (lógica). Herramienta: JUnit 5 + Mockito (automatizada).
 *
 * MongoTemplate, el publicador de eventos y el repositorio se simulan: no se necesita MongoDB real.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Auditoría en MongoDB (RNF-02)")
// Prueba el registro y consulta de eventos de auditoria.
class AuditoriaServiceTest {

    // Dependencias externas simuladas para probar auditoría sin MongoDB real.
    @Mock private MongoTemplate mongo;
    @Mock private ApplicationEventPublisher publisher;
    @Mock private AuditoriaRepository auditoriaRepository;

    // Servicio construido manualmente con las dependencias anteriores.
    private AuditoriaService service;
    // Usuario y registro usados como datos de ejemplo en varias pruebas.
    private Usuario actor;
    private LogAuditoria registro;

    @BeforeEach
    void preparar() {
        // Crea una instancia real del servicio.
        service = new AuditoriaService();
        // Inyecta los mocks en sus campos internos.
        ReflectionTestUtils.setField(service, "mongo", mongo);
        ReflectionTestUtils.setField(service, "publisher", publisher);
        ReflectionTestUtils.setField(service, "auditoriaRepository", auditoriaRepository);

        // Prepara un usuario supervisor que aparecerá en el evento.
        actor = new Usuario();
        actor.setId(7L);
        actor.setRole(Role.SUPERVISOR);

        // Prepara un registro completo con valores anteriores y nuevos.
        registro = new LogAuditoria(7L, "SUPERVISOR", "obras", 15L, "CAMBIAR_ESTADO",
                "Falta de materiales",
                Map.of("estado", "EN_PROCESO"), Map.of("estado", "SUSPENDIDA"));
    }

    @Test
    @DisplayName("registrar publica el evento con usuario, rol, entidad, acción, motivo, valores y fecha")
    void registrarPublicaElEventoCompleto() {
        // Ejecuta el registro de auditoría con todos sus datos.
        service.registrar(actor, "obras", 15L, "CAMBIAR_ESTADO", "Falta de materiales",
                Map.of("estado", "EN_PROCESO"), Map.of("estado", "SUSPENDIDA"));

        // Captura el evento publicado para inspeccionar su contenido.
        var captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(publisher).publishEvent(captor.capture());
        var evento = captor.getValue();

        // Verifica identidad del usuario y entidad afectada.
        assertThat(evento.getUsuarioId()).isEqualTo(7L);
        assertThat(evento.getUsuarioRol()).isEqualTo("SUPERVISOR");
        assertThat(evento.getEntidad()).isEqualTo("obras");
        assertThat(evento.getEntidadId()).isEqualTo(15L);
        assertThat(evento.getAccion()).isEqualTo("CAMBIAR_ESTADO");
        // Verifica motivo, cambios y existencia de la fecha de auditoría.
        assertThat(evento.getMotivo()).isEqualTo("Falta de materiales");
        assertThat(evento.getValorAnterior()).containsEntry("estado", "EN_PROCESO");
        assertThat(evento.getValorNuevo()).containsEntry("estado", "SUSPENDIDA");
        assertThat(evento.getFecha()).isNotNull();
    }

    @Test
    @DisplayName("registrar no escribe en MongoDB directamente: espera a que la transacción se confirme")
    void registrarNoGuardaAntesDelCommit() {
        // Solicita registrar el evento sin activar un guardado directo.
        service.registrar(actor, "obras", 15L, "CAMBIAR_ESTADO", null, null, null);

        // Comprueba que MongoTemplate no haya recibido ninguna interacción.
        verifyNoInteractions(mongo);
    }

    @Test
    @DisplayName("guardar se ejecuta solo después de confirmar la transacción (AFTER_COMMIT)")
    void guardarEsAfterCommit() throws NoSuchMethodException {
        // Obtiene por reflexión el método que persiste el registro.
        Method guardar = AuditoriaService.class.getMethod("guardar", LogAuditoria.class);
        var anotacion = guardar.getAnnotation(TransactionalEventListener.class);

        // Verifica que exista el listener y que espere al commit.
        assertThat(anotacion).isNotNull();
        assertThat(anotacion.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }

    @Test
    @DisplayName("guardar inserta el documento una sola vez y no hace update ni delete")
    void guardarSoloInserta() {
        // Simula que MongoDB acepta la inserción.
        when(mongo.insert(any(LogAuditoria.class))).thenReturn(registro);

        // Ejecuta el método de guardado.
        service.guardar(registro);

        // Comprueba una sola inserción y ausencia de otras operaciones.
        verify(mongo, times(1)).insert(registro);
        verifyNoMoreInteractions(mongo);
    }

    @Test
    @DisplayName("Si MongoDB falla dos veces y responde al tercer intento, el registro queda guardado")
    void reintentaYTerminaGuardando() {
        // Configura dos fallos seguidos y éxito en el tercer intento.
        when(mongo.insert(any(LogAuditoria.class)))
                .thenThrow(new RuntimeException("Mongo caído"))
                .thenThrow(new RuntimeException("Mongo caído"))
                .thenReturn(registro);

        // Ejecuta el guardado y verifica que la falla intermedia no escape.
        assertThatCode(() -> service.guardar(registro)).doesNotThrowAnyException();

        // Confirma que se realizaron exactamente tres intentos.
        verify(mongo, times(3)).insert(registro);
    }

    @Test
    @DisplayName("Si MongoDB no está disponible se intenta 3 veces y no se lanza error (el cambio de negocio se mantiene)")
    void mongoNoDisponibleSeIntentaTresVeces() {
        // Configura MongoDB para fallar en cada inserción.
        when(mongo.insert(any(LogAuditoria.class))).thenThrow(new RuntimeException("Mongo caído"));

        // Comprueba que el error interno no se propague al proceso de negocio.
        assertThatCode(() -> service.guardar(registro)).doesNotThrowAnyException();

        // Verifica los tres reintentos configurados.
        verify(mongo, times(3)).insert(registro);
    }

    @Test
    @DisplayName("LogAuditoria es inmutable: no tiene métodos set")
    void logAuditoriaNoTieneSetters() {
        // Inspecciona todos los métodos públicos de LogAuditoria.
        var setters = Arrays.stream(LogAuditoria.class.getMethods())
                .map(Method::getName)
                .filter(nombre -> nombre.startsWith("set"))
                .toList();

        // Comprueba que no existan métodos cuyo nombre comience con set.
        assertThat(setters).isEmpty();
    }

    @Test
    @DisplayName("listarPorFecha incluye completo el día 'hasta' (hora de Santiago)")
    void listarPorFechaIncluyeElDiaHasta() {
        // Define zona horaria y rango solicitado por la prueba.
        var zona = ZoneId.of("America/Santiago");
        var desde = LocalDate.of(2026, 11, 1);
        var hasta = LocalDate.of(2026, 11, 3);
        // Simula una consulta sin registros.
        when(auditoriaRepository.findByFechaBetween(any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        // Ejecuta la consulta por fechas.
        service.listarPorFecha(desde, hasta, Pageable.unpaged());

        // Captura los límites Instant enviados al repositorio.
        var inicio = ArgumentCaptor.forClass(Instant.class);
        var fin = ArgumentCaptor.forClass(Instant.class);
        verify(auditoriaRepository).findByFechaBetween(inicio.capture(), fin.capture(), any(Pageable.class));
        // Verifica el inicio del primer día y el inicio posterior al último día.
        assertThat(inicio.getValue()).isEqualTo(desde.atStartOfDay(zona).toInstant());
        assertThat(fin.getValue()).isEqualTo(LocalDate.of(2026, 11, 4).atStartOfDay(zona).toInstant());
    }
}
