package com.inacap.fyagobras.domain.presupuesto;

import com.inacap.fyagobras.domain.cliente.repository.ClienteRepository;
import com.inacap.fyagobras.domain.presupuesto.dto.*;
import com.inacap.fyagobras.domain.presupuesto.repository.PresupuestoRepository;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.presupuesto.dto.RechazarPresupuestoDTO;
import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List;

@Service
public class PresupuestoService {

    @Autowired
    private PresupuestoRepository presupuestoRepository;
    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    @Value("${app.presupuesto.umbral-aprobacion}")
    private BigDecimal umbralAprobacion;

    // HU-01: solo con un cliente válido y al menos un ítem; queda PENDIENTE
    @Transactional
    public DetallesPresupuestoDTO registrar(RegistrarPresupuestoDTO datos) {
        var cliente = clienteRepository.findById(datos.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
        var numero = String.format("PRES-%06d", presupuestoRepository.siguienteNumero());
        var guardado = presupuestoRepository.save(new Presupuesto(numero, cliente, datos.items()));
        return new DetallesPresupuestoDTO(guardado);
    }

    @Transactional(readOnly = true)
    public DetallesPresupuestoDTO obtener(Long id) {
        return new DetallesPresupuestoDTO(buscar(id));
    }

    @Transactional(readOnly = true)
    public Page<ListaPresupuestoDTO> listar(EstadoPresupuesto estado, Pageable paginacion) {
        var pagina = (estado == null)
                ? presupuestoRepository.findAll(paginacion)
                : presupuestoRepository.findByEstado(estado, paginacion);
        return pagina.map(ListaPresupuestoDTO::new);
    }

    // Para el Supervisor: presupuestos aprobados, sin montos
    @Transactional(readOnly = true)
    public List<ResumenPresupuestoDTO> aprobados() {
        return presupuestoRepository.findByEstadoOrderByFechaAprobacionDesc(EstadoPresupuesto.APROBADO)
                .stream().map(ResumenPresupuestoDTO::new).toList();
    }

    // Un presupuesto aprobado o rechazado ya no se puede modificar
    @Transactional
    public DetallesPresupuestoDTO actualizar(Long id, ModificarPresupuestoDTO datos) {
        var presupuesto = buscar(id);
        if (!presupuesto.estaPendiente()) {
            throw new ConflictoNegocioException("Solo se puede modificar un presupuesto en estado PENDIENTE.");
        }
        presupuesto.reemplazarItems(datos.items());
        return new DetallesPresupuestoDTO(presupuesto);
    }

    private Presupuesto buscar(Long id) {
        return presupuestoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Presupuesto no encontrado"));
    }

    @Transactional
    public DetallesPresupuestoDTO aprobar(Long id, Usuario actor) {
        var presupuesto = buscar(id);
        validarPendiente(presupuesto);
        validarPermisoPorUmbral(presupuesto, actor);

        var anterior = Map.<String, Object>of(
                "estado", presupuesto.getEstado().name(),
                "monto_total", presupuesto.getMontoTotal().toPlainString());

        presupuesto.aprobar();

        auditoriaService.registrar(actor, "presupuestos", presupuesto.getId(),
                "APROBAR_PRESUPUESTO", null, anterior,
                Map.of("estado", presupuesto.getEstado().name(),
                        "monto_total", presupuesto.getMontoTotal().toPlainString(),
                        "fecha_aprobacion", presupuesto.getFechaAprobacion().toString()));

        return new DetallesPresupuestoDTO(presupuesto);
    }

    @Transactional
    public DetallesPresupuestoDTO rechazar(Long id, RechazarPresupuestoDTO datos, Usuario actor) {
        var presupuesto = buscar(id);
        validarPendiente(presupuesto);
        validarPermisoPorUmbral(presupuesto, actor);

        var anterior = Map.<String, Object>of(
                "estado", presupuesto.getEstado().name(),
                "monto_total", presupuesto.getMontoTotal().toPlainString());

        presupuesto.rechazar(datos.motivo());

        auditoriaService.registrar(actor, "presupuestos", presupuesto.getId(),
                "RECHAZAR_PRESUPUESTO", datos.motivo(), anterior,
                Map.of("estado", presupuesto.getEstado().name()));

        return new DetallesPresupuestoDTO(presupuesto);
    }

    private void validarPendiente(Presupuesto presupuesto) {
        if (!presupuesto.estaPendiente()) {
            throw new ConflictoNegocioException("El presupuesto ya fue resuelto (" + presupuesto.getEstado() + ").");
        }
    }

    /** HU-03: si el monto supera el umbral, solo GERENCIA puede aprobar o rechazar. */
    private void validarPermisoPorUmbral(Presupuesto presupuesto, Usuario actor) {
        boolean superaUmbral = presupuesto.getMontoTotal().compareTo(umbralAprobacion) > 0;
        if (superaUmbral && actor.getRole() != Role.GERENCIA) {
            throw new AccessDeniedException("Presupuesto sobre el umbral: solo Gerencia puede decidir.");
        }
    }
}