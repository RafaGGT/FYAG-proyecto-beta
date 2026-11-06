package com.inacap.fyagobras.domain.factura;
import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.factura.dto.AnularPagoDTO;
import com.inacap.fyagobras.domain.factura.dto.DetallesFacturaDTO;
import com.inacap.fyagobras.domain.factura.dto.EmitirFacturaDTO;
import com.inacap.fyagobras.domain.factura.dto.RegistrarPagoDTO;
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
        emitirValidador.forEach(v -> v.validate(datos));

        var obra = obraRepository.findById(datos.obraId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));

        var factura = facturaRepository.save(new Factura(
                datos.numeroFactura().trim(), obra,
                datos.fechaEmision(), datos.fechaVencimiento(), datos.monto()));

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
        var pagina = (estadoPago == null)
                ? facturaRepository.findAll(paginacion)
                : facturaRepository.findByEstadoPago(estadoPago, paginacion);
        return pagina.map(DetallesFacturaDTO::new);
    }

    @Transactional(readOnly = true)
    public DetallesFacturaDTO obtener(Long id) {
        var factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada"));
        return new DetallesFacturaDTO(factura);
    }

    /** HU-14 y HU-30: registra el pago de una factura pendiente. */
    @Transactional
    public DetallesFacturaDTO registrarPago(Long id, RegistrarPagoDTO datos, Usuario actor) {
        var factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada"));

        pagoValidador.forEach(v -> v.validate(factura, datos));

        var anterior = Map.<String, Object>of("estado_pago", factura.getEstadoPago().name());

        factura.registrarPago(datos.fechaPago());

        auditoriaService.registrar(actor, "facturas", factura.getId(), "REGISTRAR_PAGO",
                null, anterior,
                Map.of("estado_pago", factura.getEstadoPago().name(),
                        "fecha_pago", factura.getFechaPago().toString()));

        return new DetallesFacturaDTO(factura);
    }

    /** HU-29: anula un pago registrado por error mediante una nota de crédito interna. No elimina nada. */
    @Transactional
    public DetallesFacturaDTO anularPago(Long id, AnularPagoDTO datos, Usuario actor) {
        var factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada"));

        if (factura.getEstadoPago() != EstadoPago.PAGADO) {
            throw new ConflictoNegocioException("Solo se puede anular el pago de una factura PAGADA.");
        }

        var anterior = Map.<String, Object>of(
                "estado_pago", factura.getEstadoPago().name(),
                "fecha_pago", factura.getFechaPago().toString());

        factura.anularPago(datos.motivo().trim(), actor.getId());

        auditoriaService.registrar(actor, "facturas", factura.getId(), "ANULAR_PAGO",
                datos.motivo().trim(), anterior,
                Map.of("estado_pago", factura.getEstadoPago().name()));

        return new DetallesFacturaDTO(factura);
    }

}