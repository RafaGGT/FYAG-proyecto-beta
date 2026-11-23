package com.inacap.fyagobras.presupuesto;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.cliente.Cliente;
import com.inacap.fyagobras.domain.presupuesto.EstadoPresupuesto;
import com.inacap.fyagobras.domain.presupuesto.Presupuesto;
import com.inacap.fyagobras.domain.presupuesto.PresupuestoService;
import com.inacap.fyagobras.domain.presupuesto.dto.ItemPresupuestoDTO;
import com.inacap.fyagobras.domain.presupuesto.dto.RechazarPresupuestoDTO;
import com.inacap.fyagobras.domain.presupuesto.repository.PresupuestoRepository;
import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CP: Aprobar (y rechazar) presupuesto sobre el umbral con Finanzas y con Gerencia.
 * Trazabilidad: HU-03. Tipo: funcional. Herramienta: JUnit 5 (automatizada).
 *
 * El umbral se inyecta por @Value (app.presupuesto.umbral-aprobacion); como Mockito no
 * resuelve @Value, se fija manualmente con ReflectionTestUtils al mismo valor por defecto
 * de application.properties (5.000.000) antes de cada escenario.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Umbral de aprobación de presupuestos (HU-03)")
// Prueba quién puede aprobar o rechazar según si el monto supera el umbral configurado.
class PresupuestoAprobacionTest {

    private static final Long PRESUPUESTO_ID = 10L;
    private static final BigDecimal UMBRAL = new BigDecimal("5000000");

    // Simula el repositorio de presupuestos para evitar la base de datos real.
    @Mock private PresupuestoRepository presupuestoRepository;
    // Simula el registro de auditoria para comprobar cuándo interviene.
    @Mock private AuditoriaService auditoriaService;
    // Inserta los mocks anteriores dentro del servicio que se probara.
    @InjectMocks private PresupuestoService service;

    @BeforeEach
    void preparar() {
        // Fija el umbral configurado ya que Mockito no resuelve @Value.
        ReflectionTestUtils.setField(service, "umbralAprobacion", UMBRAL);
    }

    private static Cliente cliente() {
        // Crea el cliente minimo necesario para completar el presupuesto.
        var c = new Cliente();
        c.setId(1L);
        c.setRazonSocial("Constructora Andes SpA");
        return c;
    }

    private static Presupuesto presupuestoPendiente(BigDecimal monto) {
        // Construye un presupuesto PENDIENTE con un único ítem por el monto indicado.
        var presupuesto = new Presupuesto("PRES-000001", cliente(),
                List.of(new ItemPresupuestoDTO("Instalación eléctrica", monto)));
        ReflectionTestUtils.setField(presupuesto, "id", PRESUPUESTO_ID);
        return presupuesto;
    }

    private static Usuario actor(Role rol) {
        // Crea el usuario autenticado con el rol indicado.
        var u = new Usuario();
        u.setId(2L);
        u.setRole(rol);
        return u;
    }

    @Test
    @DisplayName("Sobre el umbral, Finanzas no puede aprobar: recibe acceso denegado")
    void sobreUmbralFinanzasNoPuedeAprobar() {
        // Simula un presupuesto por sobre el umbral configurado.
        var presupuesto = presupuestoPendiente(new BigDecimal("6000000"));
        when(presupuestoRepository.findById(PRESUPUESTO_ID)).thenReturn(Optional.of(presupuesto));

        // Ejecuta la aprobación con Finanzas y verifica el rechazo por permisos.
        assertThatThrownBy(() -> service.aprobar(PRESUPUESTO_ID, actor(Role.FINANZAS)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Presupuesto sobre el umbral: solo Gerencia puede decidir.");

        // Comprueba que el presupuesto siga pendiente y sin auditoría.
        assertThat(presupuesto.getEstado()).isEqualTo(EstadoPresupuesto.PENDIENTE);
        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Sobre el umbral, Gerencia sí puede aprobar")
    void sobreUmbralGerenciaAprueba() {
        // Simula un presupuesto por sobre el umbral configurado.
        var presupuesto = presupuestoPendiente(new BigDecimal("6000000"));
        when(presupuestoRepository.findById(PRESUPUESTO_ID)).thenReturn(Optional.of(presupuesto));

        // Ejecuta la aprobación con Gerencia.
        var resultado = service.aprobar(PRESUPUESTO_ID, actor(Role.GERENCIA));

        // Verifica que el presupuesto quede aprobado.
        assertThat(resultado.estado()).isEqualTo(EstadoPresupuesto.APROBADO);
        assertThat(presupuesto.getFechaAprobacion()).isNotNull();
        // Confirma que se registre la aprobación en auditoría.
        verify(auditoriaService).registrar(any(), eq("presupuestos"), eq(PRESUPUESTO_ID),
                eq("APROBAR_PRESUPUESTO"), any(), any(), any());
    }

    @Test
    @DisplayName("Bajo el umbral, Finanzas puede aprobar sin restricción")
    void bajoUmbralFinanzasAprueba() {
        // Simula un presupuesto por debajo del umbral configurado.
        var presupuesto = presupuestoPendiente(new BigDecimal("1000000"));
        when(presupuestoRepository.findById(PRESUPUESTO_ID)).thenReturn(Optional.of(presupuesto));

        // Ejecuta la aprobación con Finanzas.
        var resultado = service.aprobar(PRESUPUESTO_ID, actor(Role.FINANZAS));

        // Verifica que Finanzas pueda resolverlo sin intervención de Gerencia.
        assertThat(resultado.estado()).isEqualTo(EstadoPresupuesto.APROBADO);
    }

    @Test
    @DisplayName("Sobre el umbral, Finanzas tampoco puede rechazar")
    void sobreUmbralFinanzasNoPuedeRechazar() {
        // Simula un presupuesto por sobre el umbral configurado.
        var presupuesto = presupuestoPendiente(new BigDecimal("7500000"));
        when(presupuestoRepository.findById(PRESUPUESTO_ID)).thenReturn(Optional.of(presupuesto));

        // Ejecuta el rechazo con Finanzas y verifica el rechazo por permisos.
        assertThatThrownBy(() -> service.rechazar(PRESUPUESTO_ID,
                new RechazarPresupuestoDTO("No corresponde al servicio"), actor(Role.FINANZAS)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Presupuesto sobre el umbral: solo Gerencia puede decidir.");

        // Comprueba que el presupuesto siga pendiente.
        assertThat(presupuesto.getEstado()).isEqualTo(EstadoPresupuesto.PENDIENTE);
    }
}