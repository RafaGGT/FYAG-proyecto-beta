package com.inacap.fyagobras.domain.obra;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.costo.repository.CostoRealRepository;
import com.inacap.fyagobras.domain.factura.EstadoPago;
import com.inacap.fyagobras.domain.factura.repository.FacturaRepository;
import com.inacap.fyagobras.domain.obra.dto.CambiarEstadoObraDTO;
import com.inacap.fyagobras.domain.obra.dto.DetallesObraDTO;
import com.inacap.fyagobras.domain.obra.dto.RegistrarObraDTO;
import com.inacap.fyagobras.domain.obra.repository.ObraRepository;
import com.inacap.fyagobras.domain.presupuesto.EstadoPresupuesto;
import com.inacap.fyagobras.domain.presupuesto.repository.PresupuestoRepository;
import com.inacap.fyagobras.domain.servicio.repository.TipoServicioRepository;
import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import com.inacap.fyagobras.domain.user.repository.UsuarioRepository;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Map;

@Service
public class ObraService {

    @Autowired private ObraRepository obraRepository;
    @Autowired private PresupuestoRepository presupuestoRepository;
    @Autowired private TipoServicioRepository tipoServicioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private AuditoriaService auditoriaService;
    @Autowired private CostoRealRepository costoRepository;
    @Autowired private FacturaRepository facturaRepository;

    /** HU-05: la obra solo se crea desde un presupuesto aprobado, con tipo vigente y técnico activo. */
    @Transactional
    public DetallesObraDTO registrar(RegistrarObraDTO datos, Usuario actor) {
        // Verifica que el presupuesto exista y esté aprobado
        var presupuesto = presupuestoRepository.findById(datos.presupuestoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Presupuesto no encontrado"));
                // si el presupuesto no está aprobado, lanza una excepción de conflicto de negocio
        if (presupuesto.getEstado() != EstadoPresupuesto.APROBADO) {
            throw new ConflictoNegocioException("Solo se puede crear una obra desde un presupuesto APROBADO.");
        } // Verifica que no exista una obra asociada al presupuesto
        if (obraRepository.existsByPresupuestoId(presupuesto.getId())) {
            // si ya existe una obra asociada al presupuesto, lanza una excepción de conflicto de negocio
            throw new ConflictoNegocioException("Ese presupuesto ya tiene una obra asociada.");
        }
        // Verifica que el tipo de servicio exista y esté activo
        var tipo = tipoServicioRepository.findById(datos.tipoServicioId())
        // si el tipo de servicio no existe, lanza una excepción de recurso no encontrado
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de servicio no encontrado"));
        if (!tipo.isActivo()) {
            throw new ConflictoNegocioException("El tipo de servicio ya no está vigente.");
        }

        var tecnico = usuarioRepository.findById(datos.tecnicoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Técnico no encontrado"));
        if (tecnico.getRole() != Role.TECNICO || !tecnico.isEnabled()) {
            throw new ConflictoNegocioException("Debe seleccionar un técnico activo.");
        }

        var obra = obraRepository.save(new Obra(datos.nombre().trim(), datos.descripcion(),
                datos.fechaInicio(), presupuesto, tipo, tecnico));

        // Registra la creación con el estado y las relaciones principales.
        auditoriaService.registrar(actor, "obras", obra.getId(), "CREAR_OBRA", null, null,
                Map.of("estado", obra.getEstado().name(),
                        "presupuesto_id", presupuesto.getId(),
                        "tecnico_id", tecnico.getId()));

        return new DetallesObraDTO(obra);
    }

    /** HU-24: el Técnico solo ve las obras donde figura como técnico asignado. */
    @Transactional(readOnly = true)
    public Page<DetallesObraDTO> listar(EstadoObra estado, Usuario actor, Pageable paginacion) {
        // Determina si el actor debe ver solo sus obras asignadas.
        boolean esTecnico = actor.getRole() == Role.TECNICO;
        Page<Obra> pagina;
        if (esTecnico) {
            pagina = (estado == null)
                    ? obraRepository.findByTecnicoId(actor.getId(), paginacion)
                    : obraRepository.findByTecnicoIdAndEstado(actor.getId(), estado, paginacion);
        } else {
            // Los demás roles pueden consultar todas las obras.
            pagina = (estado == null)
                    ? obraRepository.findAll(paginacion)
                    : obraRepository.findByEstado(estado, paginacion);
        }
        // Convierte los resultados persistentes en DTOs.
        return pagina.map(DetallesObraDTO::new);
    }

    @Transactional(readOnly = true)
    public DetallesObraDTO obtener(Long id, Usuario actor) {
        // Busca la obra solicitada.
        var obra = obraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
        // Comprueba si el técnico intenta acceder a una obra ajena.
        boolean esTecnico = actor.getRole() == Role.TECNICO;
        boolean esSuya = obra.getTecnico() != null && obra.getTecnico().getId().equals(actor.getId());
        if (esTecnico && !esSuya) {
            throw new AccessDeniedException("Solo puede consultar las obras que tiene asignadas.");
        }
        return new DetallesObraDTO(obra);
    }

    @Transactional
    public DetallesObraDTO cambiarEstado(Long id, CambiarEstadoObraDTO datos, Usuario actor) {
        // Recupera la obra y su estado actual.
        var obra = obraRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));

        // Obtiene el estado destino solicitado.
        var origen = obra.getEstado();
        var destino = datos.estado();

        // Valida la transición definida por la máquina de estados.
        if (!origen.puedePasarA(destino)) {
            throw new ConflictoNegocioException(
                    "Transición no permitida: " + origen + " a " + destino + ".");
        }
        // Suspender requiere explicar el motivo del cambio.
        if (destino == EstadoObra.SUSPENDIDA
                && (datos.motivo() == null || datos.motivo().isBlank())) {
            throw new ConflictoNegocioException("El motivo es obligatorio al suspender una obra.");
        }
        // HU-10: no se puede finalizar sin técnico asignado ni costos reales registrados
        // Finalizar requiere técnico asignado y costos reales cargados.
        if (destino == EstadoObra.FINALIZADA) {
            if (obra.getTecnico() == null) {
                throw new ConflictoNegocioException(
                        "No se puede finalizar la obra: se requiere asignar un técnico responsable.");
            }
            if (!costoRepository.existsByObraId(obra.getId())) {
                throw new ConflictoNegocioException(
                        "No se puede finalizar la obra: faltan los costos reales de materiales y horas técnicas.");
            }
        }
        // HU-11: no se puede cancelar una obra con pagos asociados
        // Cancelar está prohibido cuando existen pagos asociados.
        if (destino == EstadoObra.CANCELADA
                && facturaRepository.existsByObraIdAndEstadoPago(obra.getId(), EstadoPago.PAGADO)) {
            throw new ConflictoNegocioException(
                    "La obra no puede cancelarse por tener movimientos financieros asociados.");
        }

        // Aplica el cambio después de superar todas las reglas de negocio.
        obra.cambiarEstado(destino, datos.motivo());

        // Registra el estado anterior y el nuevo para auditoría.
        auditoriaService.registrar(actor, "obras", obra.getId(), "CAMBIAR_ESTADO",
                datos.motivo(),
                Map.of("estado", origen.name()),
                Map.of("estado", destino.name()));

        return new DetallesObraDTO(obra);
    }
}