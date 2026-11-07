package com.inacap.fyagobras.domain.factura;
import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.factura.dto.*;
import com.inacap.fyagobras.domain.factura.repository.FacturaRepository;
import com.inacap.fyagobras.domain.factura.validation.create.ValidarEmitirFactura;
import com.inacap.fyagobras.domain.factura.validation.pago.ValidarRegistrarPago;
import com.inacap.fyagobras.domain.obra.repository.ObraRepository;
import com.inacap.fyagobras.domain.user.Usuario;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class FacturaService {

    @Autowired private FacturaRepository facturaRepository;
    @Autowired private ObraRepository obraRepository;
    @Autowired private AuditoriaService auditoriaService;
    @Autowired private List<ValidarEmitirFactura> emitirValidador;
    @Autowired private List<ValidarRegistrarPago> pagoValidador;

    /** HU-13: solo se factura una obra FINALIZADA; el estado de pago inicial es PENDIENTE. */
    @Transactional
    public DetallesFacturaDTO emitir(EmitirFacturaDTO datos, Usuario actor) {
        // Ejecuta las reglas de negocio para emitir la factura.
        emitirValidador.forEach(v -> v.validate(datos));

        // Obtiene la obra asociada a la factura.
        var obra = obraRepository.findById(datos.obraId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));

        // Crea la factura con estado de pago pendiente y la persiste.
        var factura = facturaRepository.save(new Factura(
                datos.numeroFactura().trim(), obra,
                datos.fechaEmision(), datos.fechaVencimiento(), datos.monto()));

        // Registra la emisión para mantener trazabilidad de la operación.
        auditoriaService.registrar(actor, "facturas", factura.getId(), "EMITIR_FACTURA",
                null, null,
                Map.of("numero_factura", factura.getNumeroFactura(),
                        "obra_id", obra.getId(),
                        "monto", factura.getMonto().toPlainString(),
                        "estado_pago", factura.getEstadoPago().name()));

        return new DetallesFacturaDTO(factura);
    }

    @Transactional(readOnly = true)
    public Page<DetallesFacturaDTO> listar(EstadoPago estadoPago, Pageable paginacion) {
        // Filtra por estado cuando se recibe uno; de lo contrario lista todas.
        var pagina = (estadoPago == null)
                ? facturaRepository.findAll(paginacion)
                : facturaRepository.findByEstadoPago(estadoPago, paginacion);
        // Convierte las entidades de la página en DTOs.
        return pagina.map(DetallesFacturaDTO::new);
    }

    @Transactional(readOnly = true)
    public DetallesFacturaDTO obtener(Long id) {
        // Busca la factura solicitada o informa que no existe.
        var factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada"));
        return new DetallesFacturaDTO(factura);
    }

    /** HU-14 y HU-30: registra el pago de una factura pendiente. */
    @Transactional
    public DetallesFacturaDTO registrarPago(Long id, RegistrarPagoDTO datos, Usuario actor) {
        // Recupera la factura antes de validar y registrar el pago.
        var factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada"));

        // Aplica las validaciones específicas del registro de pago.
        pagoValidador.forEach(v -> v.validate(factura, datos));

        // Conserva el estado anterior para la auditoría.
        var anterior = Map.<String, Object>of("estado_pago", factura.getEstadoPago().name());

        // Cambia el estado de la factura y registra la fecha del pago.
        factura.registrarPago(datos.fechaPago());

        // Guarda el cambio funcional en la bitácora.
        auditoriaService.registrar(actor, "facturas", factura.getId(), "REGISTRAR_PAGO",
                null, anterior,
                Map.of("estado_pago", factura.getEstadoPago().name(),
                        "fecha_pago", factura.getFechaPago().toString()));

        return new DetallesFacturaDTO(factura);
    }

    /** HU-29: anula un pago registrado por error mediante una nota de crédito interna. No elimina nada. */
    @Transactional
    public DetallesFacturaDTO anularPago(Long id, AnularPagoDTO datos, Usuario actor) {
        // Recupera la factura cuyo pago se quiere anular.
        var factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada"));

        // Solo una factura pagada puede volver a estado pendiente.
        if (factura.getEstadoPago() != EstadoPago.PAGADO) {
            throw new ConflictoNegocioException("Solo se puede anular el pago de una factura PAGADA.");
        }

        // Captura los datos previos para dejar evidencia del cambio.
        var anterior = Map.<String, Object>of(
                "estado_pago", factura.getEstadoPago().name(),
                "fecha_pago", factura.getFechaPago().toString());

        // Anula el pago sin eliminar la factura ni su historial.
        factura.anularPago(datos.motivo().trim(), actor.getId());

        // Registra el motivo y el nuevo estado en auditoría.
        auditoriaService.registrar(actor, "facturas", factura.getId(), "ANULAR_PAGO",
                datos.motivo().trim(), anterior,
                Map.of("estado_pago", factura.getEstadoPago().name()));

        return new DetallesFacturaDTO(factura);
    }

    /** HU-15: clientes con al menos una factura PENDIENTE, con monto adeudado y días de atraso. */
    @Transactional(readOnly = true)
    public List<ClientePendienteDTO> clientesConPagosPendientes(String busqueda) {
        // Usa la fecha local para calcular los días de atraso.
        var hoy = LocalDate.now();
        var filtro = (busqueda == null) ? "" : busqueda.trim();

        // Consulta las facturas pendientes y transforma cada resultado en un resumen.
        return facturaRepository.clientesConPagosPendientes(filtro).stream()
                .map(c -> new ClientePendienteDTO(
                        c.getClienteId(), c.getRut(), c.getRazonSocial(),
                        c.getFacturasPendientes(), c.getMontoAdeudado(),
                        c.getVencimientoMasAntiguo(),
                        // Días de atraso desde el vencimiento más antiguo; 0 si aún no vence
                        Math.max(0, ChronoUnit.DAYS.between(c.getVencimientoMasAntiguo(), hoy))))
                .toList();
    }

}