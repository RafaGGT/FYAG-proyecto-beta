package com.inacap.fyagobras.domain.factura;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.cliente.Cliente;
import com.inacap.fyagobras.domain.factura.dto.EmitirFacturaDTO;
import com.inacap.fyagobras.domain.factura.dto.RegistrarPagoDTO;
import com.inacap.fyagobras.domain.factura.repository.FacturaRepository;
import com.inacap.fyagobras.domain.factura.validation.create.NumeroFacturaUnico;
import com.inacap.fyagobras.domain.factura.validation.create.ObraFinalizada;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CP: Facturar obra no finalizada y pagar dos veces una factura.
 * Trazabilidad: HU-13, HU-14, RF-04. Tipo: funcional. Herramienta: JUnit 5 (automatizada).
 *
 * FacturaService recibe sus validadores como listas inyectadas por Spring. Aquí se arman
 * con los validadores reales (ObraFinalizada, NumeroFacturaUnico, FacturaPendiente, FechaPagoValida)
 * y repositorios simulados, de modo que se prueban las reglas de negocio de verdad.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Facturación y pago de facturas (HU-13 / HU-14 / RF-04)")
class FacturaCobroTest {

    private static final Long OBRA_ID = 100L;
    private static final Long FACTURA_ID = 500L;
    private static final LocalDate EMISION = LocalDate.of(2026, 11, 1);
    private static final LocalDate VENCIMIENTO = LocalDate.of(2026, 12, 1);
    private static final LocalDate PAGO = LocalDate.of(2026, 11, 10);

    @Mock private FacturaRepository facturaRepository;
    @Mock private ObraRepository obraRepository;
    @Mock private AuditoriaService auditoriaService;

    private FacturaService service;
    private Usuario actor;

    @BeforeEach
    void preparar() {
        var numeroUnico = new NumeroFacturaUnico();
        ReflectionTestUtils.setField(numeroUnico, "repository", facturaRepository);
        var obraFinalizada = new ObraFinalizada();
        ReflectionTestUtils.setField(obraFinalizada, "repository", obraRepository);

        service = new FacturaService();
        ReflectionTestUtils.setField(service, "facturaRepository", facturaRepository);
        ReflectionTestUtils.setField(service, "obraRepository", obraRepository);
        ReflectionTestUtils.setField(service, "auditoriaService", auditoriaService);
        ReflectionTestUtils.setField(service, "emitirValidador", List.of(numeroUnico, obraFinalizada));
        ReflectionTestUtils.setField(service, "pagoValidador",
                List.of(new FacturaPendiente(), new FechaPagoValida()));

        actor = new Usuario();
        actor.setId(2L);
        actor.setRole(Role.FINANZAS);
    }

    private static Obra obra(EstadoObra estado) {
        var cliente = new Cliente();
        cliente.setId(1L);
        cliente.setRazonSocial("Constructora Andes SpA");

        var presupuesto = new Presupuesto("PRES-000001", cliente,
                List.of(new ItemPresupuestoDTO("Instalación", new BigDecimal("200000"))));
        ReflectionTestUtils.setField(presupuesto, "id", 10L);
        presupuesto.aprobar();

        var tipo = new TipoServicio("Instalación eléctrica");
        ReflectionTestUtils.setField(tipo, "id", 3L);

        var obra = new Obra("Instalación bodega", "Obra de prueba",
                LocalDate.of(2026, 10, 1), presupuesto, tipo, null);
        ReflectionTestUtils.setField(obra, "id", OBRA_ID);
        if (estado != EstadoObra.EN_PROCESO) {
            obra.cambiarEstado(estado, "motivo de prueba");
        }
        return obra;
    }

    private static EmitirFacturaDTO datosFactura() {
        return new EmitirFacturaDTO("F-0001", OBRA_ID, EMISION, VENCIMIENTO, new BigDecimal("200000"));
    }

    // ---------- HU-13: emitir factura ----------

    @Test
    @DisplayName("Con la obra FINALIZADA se emite la factura con estado de pago PENDIENTE")
    void obraFinalizadaSeFactura() {
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra(EstadoObra.FINALIZADA)));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> {
            Factura f = inv.getArgument(0);
            ReflectionTestUtils.setField(f, "id", FACTURA_ID);
            return f;
        });

        var resultado = service.emitir(datosFactura(), actor);

        assertThat(resultado.id()).isEqualTo(FACTURA_ID);
        assertThat(resultado.numeroFactura()).isEqualTo("F-0001");
        assertThat(resultado.estadoPago()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(resultado.anulada()).isFalse();
        assertThat(resultado.fechaPago()).isNull();
        verify(auditoriaService).registrar(eq(actor), eq("facturas"), eq(FACTURA_ID),
                eq("EMITIR_FACTURA"), isNull(), isNull(), anyMap());
    }

    @Test
    @DisplayName("Con la obra EN_PROCESO no se puede facturar")
    void obraNoFinalizadaNoSeFactura() {
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra(EstadoObra.EN_PROCESO)));

        assertThatThrownBy(() -> service.emitir(datosFactura(), actor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("Solo se puede facturar una obra en estado FINALIZADA.");

        verify(facturaRepository, never()).save(any(Factura.class));
        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Con la obra SUSPENDIDA tampoco se puede facturar")
    void obraSuspendidaNoSeFactura() {
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra(EstadoObra.SUSPENDIDA)));

        assertThatThrownBy(() -> service.emitir(datosFactura(), actor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("Solo se puede facturar una obra en estado FINALIZADA.");

        verify(facturaRepository, never()).save(any(Factura.class));
    }

    // ---------- HU-14: registrar pago ----------

    private Factura facturaPendiente() {
        var f = new Factura("F-0001", obra(EstadoObra.FINALIZADA), EMISION, VENCIMIENTO,
                new BigDecimal("200000"));
        ReflectionTestUtils.setField(f, "id", FACTURA_ID);
        return f;
    }

    @Test
    @DisplayName("Una factura PENDIENTE se paga y queda PAGADO con su fecha de pago")
    void facturaPendienteSePaga() {
        var factura = facturaPendiente();
        when(facturaRepository.findById(FACTURA_ID)).thenReturn(Optional.of(factura));

        var resultado = service.registrarPago(FACTURA_ID, new RegistrarPagoDTO(PAGO), actor);

        assertThat(resultado.estadoPago()).isEqualTo(EstadoPago.PAGADO);
        assertThat(resultado.fechaPago()).isEqualTo(PAGO);
        verify(auditoriaService).registrar(eq(actor), eq("facturas"), eq(FACTURA_ID),
                eq("REGISTRAR_PAGO"), isNull(), anyMap(), anyMap());
    }

    @Test
    @DisplayName("Una factura PAGADA no se vuelve a pagar: el segundo pago se rechaza y no cambia nada")
    void facturaPagadaNoSePagaDosVeces() {
        var factura = facturaPendiente();
        when(facturaRepository.findById(FACTURA_ID)).thenReturn(Optional.of(factura));

        service.registrarPago(FACTURA_ID, new RegistrarPagoDTO(PAGO), actor);

        var segundoPago = new RegistrarPagoDTO(PAGO.plusDays(5));
        assertThatThrownBy(() -> service.registrarPago(FACTURA_ID, segundoPago, actor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("La factura ya se encuentra pagada.");

        // Sigue pagada con la fecha del primer pago y se auditó un solo pago
        assertThat(factura.getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
        assertThat(factura.getFechaPago()).isEqualTo(PAGO);
        verify(auditoriaService, times(1)).registrar(any(), eq("facturas"), eq(FACTURA_ID),
                eq("REGISTRAR_PAGO"), any(), anyMap(), anyMap());
    }
}
