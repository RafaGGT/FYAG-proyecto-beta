package com.inacap.fyagobras.presupuesto;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.cliente.Cliente;
import com.inacap.fyagobras.domain.presupuesto.EstadoPresupuesto;
import com.inacap.fyagobras.domain.presupuesto.Presupuesto;
import com.inacap.fyagobras.domain.presupuesto.PresupuestoService;
import com.inacap.fyagobras.domain.presupuesto.dto.ItemPresupuestoDTO;
import com.inacap.fyagobras.domain.presupuesto.dto.ModificarPresupuestoDTO;
import com.inacap.fyagobras.domain.presupuesto.dto.RechazarPresupuestoDTO;
import com.inacap.fyagobras.domain.presupuesto.repository.PresupuestoRepository;
import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CP: Un presupuesto resuelto no se modifica ni se resuelve dos veces; el umbral se aplica solo cuando
 * el monto lo supera; aprobar exige cliente e ítems.
 * Trazabilidad: HU-02, HU-03, RF-01, OWASP A04. Tipo: funcional (unitaria). Herramienta: JUnit 5 (automatizada).
 *
 * Complementa a PresupuestoAprobacionTest y PresupuestoRegistroTest. El umbral se fija con
 * ReflectionTestUtils al mismo valor por defecto de application.properties (5.000.000).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Presupuestos resueltos y límites del umbral (HU-02 / HU-03)")
// Prueba las reglas que protegen un presupuesto después de aprobado o rechazado.
class PresupuestoInmutabilidadTest {

    private static final Long PRESUPUESTO_ID = 10L;
    private static final BigDecimal UMBRAL = new BigDecimal("5000000");

    // Simula el repositorio y la auditoría para no usar bases de datos reales.
    @Mock private PresupuestoRepository presupuestoRepository;
    @Mock private AuditoriaService auditoriaService;
    // Servicio real que se prueba con los mocks anteriores.
    @InjectMocks private PresupuestoService service;

    @BeforeEach
    void preparar() {
        // Mockito no resuelve @Value, por eso el umbral se fija manualmente.
        ReflectionTestUtils.setField(service, "umbralAprobacion", UMBRAL);
    }

    private static Cliente cliente() {
        // Cliente mínimo requerido por el presupuesto.
        var c = new Cliente();
        c.setId(1L);
        c.setRazonSocial("Constructora Andes SpA");
        return c;
    }

    private static Presupuesto presupuesto(EstadoPresupuesto estado, BigDecimal monto) {
        // Construye un presupuesto con un único ítem y lo lleva al estado pedido.
        var p = new Presupuesto("PRES-000001", cliente(),
                List.of(new ItemPresupuestoDTO("Instalación eléctrica", monto)));
        ReflectionTestUtils.setField(p, "id", PRESUPUESTO_ID);
        if (estado == EstadoPresupuesto.APROBADO) {
            p.aprobar();
        } else if (estado == EstadoPresupuesto.RECHAZADO) {
            p.rechazar("Monto fuera de rango");
        }
        return p;
    }

    private static Usuario actor(Role rol) {
        // Usuario autenticado con el rol indicado.
        var u = new Usuario();
        u.setId(2L);
        u.setRole(rol);
        return u;
    }

    // ---------- HU-02: un presupuesto resuelto no se modifica ----------

    @ParameterizedTest(name = "Presupuesto {0}: no se puede modificar")
    @EnumSource(value = EstadoPresupuesto.class, names = {"APROBADO", "RECHAZADO"})
    @DisplayName("Un presupuesto APROBADO o RECHAZADO no se puede modificar y conserva su monto")
    void presupuestoResueltoNoSeModifica(EstadoPresupuesto estado) {
        var presupuesto = presupuesto(estado, new BigDecimal("200000"));
        when(presupuestoRepository.findById(PRESUPUESTO_ID)).thenReturn(Optional.of(presupuesto));

        var intento = new ModificarPresupuestoDTO(
                List.of(new ItemPresupuestoDTO("Ítem cambiado", new BigDecimal("1"))));

        // Intenta cambiar los ítems de un presupuesto ya resuelto.
        assertThatThrownBy(() -> service.actualizar(PRESUPUESTO_ID, intento))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("Solo se puede modificar un presupuesto en estado PENDIENTE.");

        // El monto original queda intacto.
        assertThat(presupuesto.getMontoTotal()).isEqualByComparingTo("200000");
        assertThat(presupuesto.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("Un presupuesto PENDIENTE sí se modifica y su monto se recalcula como suma de ítems")
    void presupuestoPendienteSeModificaYRecalculaMonto() {
        var presupuesto = presupuesto(EstadoPresupuesto.PENDIENTE, new BigDecimal("200000"));
        when(presupuestoRepository.findById(PRESUPUESTO_ID)).thenReturn(Optional.of(presupuesto));

        var cambio = new ModificarPresupuestoDTO(List.of(
                new ItemPresupuestoDTO("Instalación", new BigDecimal("100000")),
                new ItemPresupuestoDTO("Materiales", new BigDecimal("50000"))));

        var resultado = service.actualizar(PRESUPUESTO_ID, cambio);

        // Comprueba el nuevo total y que el estado siga pendiente.
        assertThat(resultado.montoTotal()).isEqualByComparingTo("150000");
        assertThat(resultado.estado()).isEqualTo(EstadoPresupuesto.PENDIENTE);
    }

    // ---------- Un presupuesto no se resuelve dos veces ----------

    @ParameterizedTest(name = "Presupuesto {0}: no se aprueba otra vez")
    @EnumSource(value = EstadoPresupuesto.class, names = {"APROBADO", "RECHAZADO"})
    @DisplayName("Aprobar un presupuesto ya resuelto se rechaza y no genera auditoría")
    void noSeApruebaDosVeces(EstadoPresupuesto estado) {
        when(presupuestoRepository.findById(PRESUPUESTO_ID))
                .thenReturn(Optional.of(presupuesto(estado, new BigDecimal("200000"))));

        assertThatThrownBy(() -> service.aprobar(PRESUPUESTO_ID, actor(Role.GERENCIA)))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("El presupuesto ya fue resuelto (" + estado + ").");

        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest(name = "Presupuesto {0}: no se rechaza otra vez")
    @EnumSource(value = EstadoPresupuesto.class, names = {"APROBADO", "RECHAZADO"})
    @DisplayName("Rechazar un presupuesto ya resuelto se rechaza y no genera auditoría")
    void noSeRechazaDosVeces(EstadoPresupuesto estado) {
        when(presupuestoRepository.findById(PRESUPUESTO_ID))
                .thenReturn(Optional.of(presupuesto(estado, new BigDecimal("200000"))));

        assertThatThrownBy(() -> service.rechazar(PRESUPUESTO_ID,
                new RechazarPresupuestoDTO("Otro motivo"), actor(Role.GERENCIA)))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("El presupuesto ya fue resuelto (" + estado + ").");

        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Aprobar un presupuesto que no existe lanza RecursoNoEncontradoException")
    void aprobarPresupuestoInexistente() {
        when(presupuestoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.aprobar(99L, actor(Role.FINANZAS)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Presupuesto no encontrado");
    }

    // ---------- HU-03: el umbral es estricto ----------

    @Test
    @DisplayName("Un monto exactamente igual al umbral NO exige a Gerencia: Finanzas puede aprobar")
    void montoIgualAlUmbralLoApruebaFinanzas() {
        var presupuesto = presupuesto(EstadoPresupuesto.PENDIENTE, UMBRAL);
        when(presupuestoRepository.findById(PRESUPUESTO_ID)).thenReturn(Optional.of(presupuesto));

        var resultado = service.aprobar(PRESUPUESTO_ID, actor(Role.FINANZAS));

        assertThat(resultado.estado()).isEqualTo(EstadoPresupuesto.APROBADO);
    }

    @Test
    @DisplayName("Un monto un peso sobre el umbral exige a Gerencia: Finanzas recibe acceso denegado")
    void montoUnPesoSobreElUmbralExigeGerencia() {
        var presupuesto = presupuesto(EstadoPresupuesto.PENDIENTE, UMBRAL.add(BigDecimal.ONE));
        when(presupuestoRepository.findById(PRESUPUESTO_ID)).thenReturn(Optional.of(presupuesto));

        assertThatThrownBy(() -> service.aprobar(PRESUPUESTO_ID, actor(Role.FINANZAS)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Presupuesto sobre el umbral: solo Gerencia puede decidir.");

        assertThat(presupuesto.getEstado()).isEqualTo(EstadoPresupuesto.PENDIENTE);
    }

    @Test
    @DisplayName("Gerencia rechaza un presupuesto sobre el umbral y el motivo queda en la auditoría")
    void gerenciaRechazaSobreUmbralConMotivoAuditado() {
        var presupuesto = presupuesto(EstadoPresupuesto.PENDIENTE, new BigDecimal("6000000"));
        when(presupuestoRepository.findById(PRESUPUESTO_ID)).thenReturn(Optional.of(presupuesto));
        var gerencia = actor(Role.GERENCIA);

        var resultado = service.rechazar(PRESUPUESTO_ID, new RechazarPresupuestoDTO("Fuera de presupuesto"), gerencia);

        // Comprueba el estado y el motivo conservado en el presupuesto.
        assertThat(resultado.estado()).isEqualTo(EstadoPresupuesto.RECHAZADO);
        assertThat(presupuesto.getMotivoRechazo()).isEqualTo("Fuera de presupuesto");
        // Confirma que el rechazo quede registrado con su motivo.
        verify(auditoriaService).registrar(eq(gerencia), eq("presupuestos"), eq(PRESUPUESTO_ID),
                eq("RECHAZAR_PRESUPUESTO"), eq("Fuera de presupuesto"), anyMap(), anyMap());
    }

    // ---------- HU-02: aprobar exige cliente e ítems ----------

    @Test
    @DisplayName("Un presupuesto sin ítems no se puede aprobar y sigue PENDIENTE")
    void sinItemsNoSePuedeAprobar() {
        var sinItems = new Presupuesto("PRES-000002", cliente(), List.of());

        assertThatThrownBy(sinItems::aprobar)
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("Para aprobar, el presupuesto debe tener cliente y al menos un ítem con monto.");

        assertThat(sinItems.getEstado()).isEqualTo(EstadoPresupuesto.PENDIENTE);
        assertThat(sinItems.getFechaAprobacion()).isNull();
    }

    @Test
    @DisplayName("Un presupuesto sin cliente no se puede aprobar y sigue PENDIENTE")
    void sinClienteNoSePuedeAprobar() {
        var sinCliente = new Presupuesto("PRES-000003", null,
                List.of(new ItemPresupuestoDTO("Instalación", new BigDecimal("1000"))));

        assertThatThrownBy(sinCliente::aprobar)
                .isInstanceOf(ConflictoNegocioException.class);

        assertThat(sinCliente.getEstado()).isEqualTo(EstadoPresupuesto.PENDIENTE);
    }
}