package com.inacap.fyagobras.obra;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.cliente.Cliente;
import com.inacap.fyagobras.domain.costo.repository.CostoRealRepository;
import com.inacap.fyagobras.domain.factura.EstadoPago;
import com.inacap.fyagobras.domain.factura.repository.FacturaRepository;
import com.inacap.fyagobras.domain.notificacion.NotificacionService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CP: Transiciones de estado de la obra (máquina de estados), suspensión sin motivo
 * y cancelación con pagos asociados.
 * Trazabilidad: HU-09, HU-11, HU-12, RF-03. Tipo: funcional. Herramienta: JUnit 5 (automatizada).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Transiciones de estado de la obra (HU-09 / HU-11 / HU-12)")
// Prueba las reglas de la máquina de estados que no cubre la finalización de la obra.
class ObraTransicionesTest {

    private static final Long OBRA_ID = 100L;

    // Repositorios y colaboradores simulados que conectan al servicio real.
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

    @BeforeEach
    void preparar() {
        // Crea un supervisor con los datos requeridos por las validaciones.
        actor = new Usuario();
        actor.setId(1L);
        actor.setRole(Role.SUPERVISOR);
    }

    private static Obra obraEnEstado(EstadoObra estado) {
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

        // Construye la obra, que nace EN_PROCESO, y la lleva al estado solicitado si corresponde.
        var obra = new Obra("Instalación bodega", "Obra de prueba",
                LocalDate.of(2026, 10, 1), presupuesto, tipo, null);
        ReflectionTestUtils.setField(obra, "id", OBRA_ID);
        if (estado != EstadoObra.EN_PROCESO) {
            obra.cambiarEstado(estado, "motivo de prueba");
        }
        return obra;
    }

    @Test
    @DisplayName("Una transición fuera del diagrama de estados se rechaza (FINALIZADA no puede volver a EN_PROCESO)")
    void transicionNoPermitidaSeRechaza() {
        // Prepara una obra ya finalizada, estado terminal del que no se puede salir.
        var obra = obraEnEstado(EstadoObra.FINALIZADA);
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra));

        var intento = new CambiarEstadoObraDTO(EstadoObra.EN_PROCESO, null);

        // Ejecuta el intento de reabrir la obra y verifica el rechazo.
        assertThatThrownBy(() -> service.cambiarEstado(OBRA_ID, intento, actor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("Transición no permitida: FINALIZADA a EN_PROCESO.");

        // Comprueba que la obra siga FINALIZADA y que no se audite nada.
        assertThat(obra.getEstado()).isEqualTo(EstadoObra.FINALIZADA);
        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Suspender sin motivo se bloquea y la obra sigue EN_PROCESO")
    void suspenderSinMotivoSeBloquea() {
        // Prepara una obra en proceso válida para suspender.
        var obra = obraEnEstado(EstadoObra.EN_PROCESO);
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra));

        var sinMotivo = new CambiarEstadoObraDTO(EstadoObra.SUSPENDIDA, "   ");

        // Ejecuta la suspensión sin motivo (en blanco) y verifica el rechazo.
        assertThatThrownBy(() -> service.cambiarEstado(OBRA_ID, sinMotivo, actor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("El motivo es obligatorio al suspender una obra.");

        // Comprueba que la obra no haya cambiado de estado ni se haya auditado.
        assertThat(obra.getEstado()).isEqualTo(EstadoObra.EN_PROCESO);
        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Suspender con motivo pasa a SUSPENDIDA y conserva el motivo")
    void suspenderConMotivoCambiaDeEstado() {
        // Prepara una obra en proceso válida para suspender.
        var obra = obraEnEstado(EstadoObra.EN_PROCESO);
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra));

        var conMotivo = new CambiarEstadoObraDTO(EstadoObra.SUSPENDIDA, "Falta de materiales");

        // Ejecuta la suspensión con motivo informado.
        var resultado = service.cambiarEstado(OBRA_ID, conMotivo, actor);

        // Verifica el nuevo estado y que el motivo haya quedado registrado.
        assertThat(resultado.estado()).isEqualTo(EstadoObra.SUSPENDIDA);
        assertThat(resultado.motivoSuspension()).isEqualTo("Falta de materiales");
        verify(auditoriaService).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Cancelar una obra con pagos asociados se bloquea")
    void cancelarConPagosAsociadosSeBloquea() {
        // Prepara una obra en proceso con al menos un pago ya registrado.
        var obra = obraEnEstado(EstadoObra.EN_PROCESO);
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra));
        when(facturaRepository.existsByObraIdAndEstadoPago(OBRA_ID, EstadoPago.PAGADO)).thenReturn(true);

        var cancelar = new CambiarEstadoObraDTO(EstadoObra.CANCELADA, null);

        // Ejecuta la cancelación y verifica el rechazo por movimientos financieros.
        assertThatThrownBy(() -> service.cambiarEstado(OBRA_ID, cancelar, actor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("La obra no puede cancelarse por tener movimientos financieros asociados.");

        // Comprueba que la obra siga EN_PROCESO.
        assertThat(obra.getEstado()).isEqualTo(EstadoObra.EN_PROCESO);
        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Cancelar una obra sin pagos asociados se permite")
    void cancelarSinPagosAsociadosSePermite() {
        // Prepara una obra en proceso sin pagos registrados.
        var obra = obraEnEstado(EstadoObra.EN_PROCESO);
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra));
        when(facturaRepository.existsByObraIdAndEstadoPago(OBRA_ID, EstadoPago.PAGADO)).thenReturn(false);

        var cancelar = new CambiarEstadoObraDTO(EstadoObra.CANCELADA, null);

        // Ejecuta la cancelación, que ahora sí debe completarse.
        var resultado = service.cambiarEstado(OBRA_ID, cancelar, actor);

        // Verifica que la obra quede cancelada.
        assertThat(resultado.estado()).isEqualTo(EstadoObra.CANCELADA);
    }
}