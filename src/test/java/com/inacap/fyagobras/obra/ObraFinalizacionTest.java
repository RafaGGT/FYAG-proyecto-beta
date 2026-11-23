package com.inacap.fyagobras.obra;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.cliente.Cliente;
import com.inacap.fyagobras.domain.costo.repository.CostoRealRepository;
import com.inacap.fyagobras.domain.factura.repository.FacturaRepository;
import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.obra.Obra;
import com.inacap.fyagobras.domain.obra.ObraService;
import com.inacap.fyagobras.domain.obra.dto.CambiarEstadoObraDTO;
import com.inacap.fyagobras.domain.obra.repository.ObraRepository;
import com.inacap.fyagobras.domain.presupuesto.Presupuesto;
import com.inacap.fyagobras.domain.presupuesto.dto.ItemPresupuestoDTO;
import com.inacap.fyagobras.domain.presupuesto.repository.PresupuestoRepository;
import com.inacap.fyagobras.domain.servicio.TipoServicio;
import com.inacap.fyagobras.domain.servicio.repository.TipoServicioRepository;
import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import com.inacap.fyagobras.domain.user.repository.UsuarioRepository;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.inacap.fyagobras.domain.notificacion.NotificacionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CP: Finalizar obra sin técnico o sin costos reales.
 * Trazabilidad: HU-10, HU-04, RF-03. Tipo: funcional. Herramienta: JUnit 5 (automatizada).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Finalización de obras (HU-10 / HU-04 / RF-03)")
// Prueba las reglas requeridas para finalizar una obra.
class ObraFinalizacionTest {

    // Identificador fijo usado para localizar la obra en todos los escenarios.
    private static final Long OBRA_ID = 100L;

    // Repositorios simulados que permiten controlar las consultas del servicio.
    @Mock private ObraRepository obraRepository;
    @Mock private PresupuestoRepository presupuestoRepository;
    @Mock private TipoServicioRepository tipoServicioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private CostoRealRepository costoRepository;
    @Mock private FacturaRepository facturaRepository;
    @Mock private NotificacionService notificacionService;
    // Servicio real al que se conectan los mocks anteriores.
    @InjectMocks private ObraService service;

    // Actor utilizado como usuario autenticado para auditar el cambio.
    private Usuario actor;
    // Solicitud que indica que la obra debe pasar a FINALIZADA.
    private final CambiarEstadoObraDTO finalizar = new CambiarEstadoObraDTO(EstadoObra.FINALIZADA, null);

    @BeforeEach
    void preparar() {
        // Crea un supervisor con los datos requeridos por las validaciones.
        actor = new Usuario();
        actor.setId(1L);
        actor.setRole(Role.SUPERVISOR);
    }

    private static Obra obraEnProceso(Usuario tecnico) {
        // Construye el cliente asociado al presupuesto de la obra.
        var cliente = new Cliente();
        cliente.setId(1L);
        cliente.setRazonSocial("Constructora Andes SpA");

        // Crea un presupuesto aprobado y le asigna un identificador interno.
        var presupuesto = new Presupuesto("PRES-000001", cliente,
                List.of(new ItemPresupuestoDTO("Instalación", new BigDecimal("200000"))));
        ReflectionTestUtils.setField(presupuesto, "id", 10L);
        presupuesto.aprobar();

        // Crea el tipo de servicio utilizado por la obra.
        var tipo = new TipoServicio("Instalación eléctrica");
        ReflectionTestUtils.setField(tipo, "id", 3L);

        // Construye la obra en estado EN_PROCESO con el técnico recibido.
        var obra = new Obra("Instalación bodega", "Obra de prueba",
                LocalDate.of(2026, 11, 1), presupuesto, tipo, tecnico);
        ReflectionTestUtils.setField(obra, "id", OBRA_ID);
        return obra;
    }

    private static Usuario tecnico() {
        // Crea un técnico activo con información mínima para la prueba.
        var t = new Usuario();
        t.setId(5L);
        t.setNombre("Juan");
        t.setApellido("Pérez");
        t.setRole(Role.TECNICO);
        return t;
    }

    @Test
    @DisplayName("Sin técnico asignado se bloquea y la obra sigue EN_PROCESO")
    void sinTecnicoSeBloquea() {
        // Prepara una obra válida, pero sin técnico responsable.
        var obra = obraEnProceso(null);
        // Simula que el repositorio encuentra la obra por su ID.
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra));

        // Ejecuta la finalización y verifica la excepción y su mensaje.
        assertThatThrownBy(() -> service.cambiarEstado(OBRA_ID, finalizar, actor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("No se puede finalizar la obra: se requiere asignar un técnico responsable.");

        // Comprueba que el rechazo no haya cambiado el estado ni la fecha de término.
        assertThat(obra.getEstado()).isEqualTo(EstadoObra.EN_PROCESO);
        assertThat(obra.getFechaTermino()).isNull();
        // Comprueba que no se registre auditoría para un cambio rechazado.
        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Sin costos reales registrados se bloquea y la obra sigue EN_PROCESO")
    void sinCostosRealesSeBloquea() {
        // Prepara una obra con técnico, pero sin costos registrados.
        var obra = obraEnProceso(tecnico());
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra));
        // Indica al repositorio que no existen costos reales para esta obra.
        when(costoRepository.existsByObraId(OBRA_ID)).thenReturn(false);

        // Ejecuta la finalización y verifica que sea rechazada.
        assertThatThrownBy(() -> service.cambiarEstado(OBRA_ID, finalizar, actor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("No se puede finalizar la obra: faltan los costos reales de materiales y horas técnicas.");

        // Confirma que el estado y la fecha no hayan sido modificados.
        assertThat(obra.getEstado()).isEqualTo(EstadoObra.EN_PROCESO);
        assertThat(obra.getFechaTermino()).isNull();
        // Comprueba que no exista auditoría para el intento bloqueado.
        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Con técnico y costos reales la obra pasa a FINALIZADA con fecha de término")
    void conTodoCompletoPasaAFinalizada() {
        // Prepara una obra con técnico y simula que existen costos reales.
        var obra = obraEnProceso(tecnico());
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra));
        when(costoRepository.existsByObraId(OBRA_ID)).thenReturn(true);

        // Ejecuta el cambio de estado completo.
        var resultado = service.cambiarEstado(OBRA_ID, finalizar, actor);

        // Verifica el resultado devuelto y la fecha asignada.
        assertThat(resultado.estado()).isEqualTo(EstadoObra.FINALIZADA);
        assertThat(resultado.fechaTermino()).isEqualTo(LocalDate.now());
        // Verifica que la entidad también haya cambiado su estado.
        assertThat(obra.getEstado()).isEqualTo(EstadoObra.FINALIZADA);
        // Confirma que se registre el cambio en la auditoría.
        verify(auditoriaService).registrar(eq(actor), eq("obras"), eq(OBRA_ID), eq("CAMBIAR_ESTADO"),
                isNull(), anyMap(), anyMap());
    }
}
