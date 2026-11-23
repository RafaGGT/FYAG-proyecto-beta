package com.inacap.fyagobras.factura;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.cliente.Cliente;
import com.inacap.fyagobras.domain.factura.EstadoPago;
import com.inacap.fyagobras.domain.factura.Factura;
import com.inacap.fyagobras.domain.factura.FacturaService;
import com.inacap.fyagobras.domain.factura.dto.AnularPagoDTO;
import com.inacap.fyagobras.domain.factura.dto.RegistrarPagoDTO;
import com.inacap.fyagobras.domain.factura.repository.FacturaRepository;
import com.inacap.fyagobras.domain.factura.validation.pago.FacturaPendiente;
import com.inacap.fyagobras.domain.factura.validation.pago.FechaPagoValida;
import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.obra.Obra;
import com.inacap.fyagobras.domain.obra.repository.ObraRepository;
import com.inacap.fyagobras.domain.presupuesto.Presupuesto;
import com.inacap.fyagobras.domain.presupuesto.dto.ItemPresupuestoDTO;
import com.inacap.fyagobras.domain.servicio.TipoServicio;
import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * CP: Anulación de pago (nota de crédito interna) y fecha de pago anterior a la emisión.
 * Trazabilidad: HU-29, HU-30. Tipo: funcional. Herramienta: JUnit 5 (automatizada).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Anulación de pago y validación de fecha de pago (HU-29 / HU-30)")
// Prueba la reversa de un pago mal registrado y el rechazo de fechas de pago inválidas.
class FacturaAnulacionPagoTest {

    private static final Long OBRA_ID = 100L;
    private static final Long FACTURA_ID = 500L;
    private static final LocalDate EMISION = LocalDate.of(2026, 11, 1);
    private static final LocalDate VENCIMIENTO = LocalDate.of(2026, 12, 1);
    private static final LocalDate PAGO = LocalDate.of(2026, 11, 10);

    // Repositorio simulado para consultar facturas.
    @Mock private FacturaRepository facturaRepository;
    @Mock private AuditoriaService auditoriaService;

    // Servicio construido manualmente porque los validadores deben controlarse en la prueba.
    private FacturaService service;
    // Usuario financiero que ejecuta la anulación o el pago.
    private Usuario actor;

    @BeforeEach
    void preparar() {
        // Construye el servicio y conecta sus dependencias manualmente.
        service = new FacturaService();
        ReflectionTestUtils.setField(service, "facturaRepository", facturaRepository);
        ReflectionTestUtils.setField(service, "auditoriaService", auditoriaService);
        // Usa las reglas reales de pago (anularPago no depende de ningún validador).
        ReflectionTestUtils.setField(service, "pagoValidador",
                List.of(new FacturaPendiente(), new FechaPagoValida()));

        // Crea el usuario autenticado con rol FINANZAS.
        actor = new Usuario();
        actor.setId(2L);
        actor.setRole(Role.FINANZAS);
    }

    private static Obra obraFinalizada() {
        // Construye el cliente asociado al presupuesto.
        var cliente = new Cliente();
        cliente.setId(1L);
        cliente.setRazonSocial("Constructora Andes SpA");

        // Crea y aprueba el presupuesto usado por la obra.
        var presupuesto = new Presupuesto("PRES-000001", cliente,
                List.of(new ItemPresupuestoDTO("Instalación", new BigDecimal("200000"))));
        ReflectionTestUtils.setField(presupuesto, "id", 10L);
        presupuesto.aprobar();

        // Crea el tipo de servicio y asigna sus identificadores.
        var tipo = new TipoServicio("Instalación eléctrica");
        ReflectionTestUtils.setField(tipo, "id", 3L);

        // Construye la obra y la deja FINALIZADA, requisito para poder facturar.
        var obra = new Obra("Instalación bodega", "Obra de prueba",
                LocalDate.of(2026, 10, 1), presupuesto, tipo, null);
        ReflectionTestUtils.setField(obra, "id", OBRA_ID);
        obra.cambiarEstado(EstadoObra.FINALIZADA, "motivo de prueba");
        return obra;
    }

    private static Factura facturaPendiente() {
        // Construye una factura pendiente asociada a una obra finalizada.
        var f = new Factura("F-0001", obraFinalizada(), EMISION, VENCIMIENTO, new BigDecimal("200000"));
        ReflectionTestUtils.setField(f, "id", FACTURA_ID);
        return f;
    }

    private static Factura facturaPagada() {
        // Construye una factura ya pagada para probar su anulación.
        var f = facturaPendiente();
        f.registrarPago(PAGO);
        return f;
    }

    // ---------- HU-29: anular pago ----------

    @Test
    @DisplayName("Anular el pago de una factura PAGADA la devuelve a PENDIENTE y conserva el motivo")
    void anularPagoDeFacturaPagadaVuelveAPendiente() {
        // Prepara una factura previamente pagada.
        var factura = facturaPagada();
        when(facturaRepository.findById(FACTURA_ID)).thenReturn(Optional.of(factura));

        // Ejecuta la anulación del pago con el motivo correspondiente.
        var resultado = service.anularPago(FACTURA_ID, new AnularPagoDTO("Pago registrado dos veces por error"), actor);

        // Verifica que la factura vuelva a estar pendiente y sin fecha de pago.
        assertThat(resultado.estadoPago()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(resultado.fechaPago()).isNull();
        assertThat(resultado.motivoAnulacion()).isEqualTo("Pago registrado dos veces por error");
        // Comprueba que la entidad conserve quién y cuándo anuló el pago.
        assertThat(factura.getAnuladaPor()).isEqualTo(actor.getId());
        assertThat(factura.getFechaAnulacion()).isNotNull();
    }

    @Test
    @DisplayName("No se puede anular el pago de una factura que todavía está PENDIENTE")
    void noSePuedeAnularPagoDeFacturaPendiente() {
        // Prepara una factura que nunca fue pagada.
        var factura = facturaPendiente();
        when(facturaRepository.findById(FACTURA_ID)).thenReturn(Optional.of(factura));

        // Ejecuta la anulación y verifica el rechazo.
        assertThatThrownBy(() -> service.anularPago(FACTURA_ID,
                new AnularPagoDTO("Intento sobre una factura sin pago"), actor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("Solo se puede anular el pago de una factura PAGADA.");

        // Comprueba que la factura siga pendiente y sin datos de anulación.
        assertThat(factura.getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(factura.getMotivoAnulacion()).isNull();
    }

    // ---------- HU-30: fecha de pago no puede ser anterior a la emisión ----------

    @Test
    @DisplayName("Una fecha de pago anterior a la emisión se rechaza y no modifica la factura")
    void fechaPagoAnteriorALaEmisionSeRechaza() {
        // Prepara una factura pendiente válida para pagar.
        var factura = facturaPendiente();
        when(facturaRepository.findById(FACTURA_ID)).thenReturn(Optional.of(factura));

        // Intenta registrar un pago con fecha anterior a la de emisión.
        var fechaInvalida = new RegistrarPagoDTO(EMISION.minusDays(1));
        assertThatThrownBy(() -> service.registrarPago(FACTURA_ID, fechaInvalida, actor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("La fecha de pago no puede ser anterior a la fecha de emisión de la factura.");

        // Comprueba que la factura siga pendiente y sin fecha de pago registrada.
        assertThat(factura.getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(factura.getFechaPago()).isNull();
    }
}