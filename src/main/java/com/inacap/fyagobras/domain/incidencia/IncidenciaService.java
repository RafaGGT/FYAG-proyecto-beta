package com.inacap.fyagobras.domain.incidencia;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.incidencia.dto.DetallesIncidenciaDTO;
import com.inacap.fyagobras.domain.incidencia.dto.RegistrarIncidenciaDTO;
import com.inacap.fyagobras.domain.notificacion.NotificacionService;
import com.inacap.fyagobras.domain.notificacion.TipoNotificacion;
import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.obra.Obra;
import com.inacap.fyagobras.domain.obra.repository.ObraRepository;
import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
// Clase IncidenciaService: implementa la funcionalidad del dominio.
public class IncidenciaService {

    @Autowired private IncidenciaObraRepository repository;
    @Autowired private ObraRepository obraRepository;
    @Autowired private AuditoriaService auditoriaService;
    @Autowired private NotificacionService notificacionService;

    /** El Técnico solo comenta en sus obras. La obra cerrada ya no admite novedades. */
    @Transactional
    // Ejecuta la operacion registrar.
    public DetallesIncidenciaDTO registrar(Long obraId, RegistrarIncidenciaDTO datos, Usuario actor) {
        var obra = buscarObra(obraId, actor);
        if (obra.getEstado() == EstadoObra.FINALIZADA || obra.getEstado() == EstadoObra.CANCELADA) {
            throw new ConflictoNegocioException("No se pueden agregar novedades a una obra " + obra.getEstado() + ".");
        }

        var texto = datos.texto().trim();
        var incidencia = repository.save(new IncidenciaObra(obra, actor, datos.tipo(), texto));

        // Misma bitácora que el resto del sistema: queda quién, cuándo y qué
        auditoriaService.registrar(actor, "obras", obra.getId(), "REGISTRAR_INCIDENCIA", texto,
                null, Map.of("tipo", datos.tipo().name()));

        avisar(obra, datos.tipo(), texto, actor);
        return new DetallesIncidenciaDTO(incidencia);
    }

    @Transactional(readOnly = true)
    // Ejecuta la operacion listar.
    public List<DetallesIncidenciaDTO> listar(Long obraId, Usuario actor) {
        buscarObra(obraId, actor);
        return repository.findByObraIdOrderByFechaDesc(obraId).stream().map(DetallesIncidenciaDTO::new).toList();
    }

    // Ejecuta la operacion avisar.
    private void avisar(Obra obra, TipoIncidencia tipo, String texto, Usuario actor) {
        var resumen = texto.length() <= 100 ? texto : texto.substring(0, 99) + "…";
        var mensaje = tipo.etiqueta() + " en «" + obra.getNombre() + "» ("
                + actor.getNombre() + " " + actor.getApellido() + "): " + resumen;

        if (actor.getRole() == Role.TECNICO) {
            // El técnico avisa al supervisor; si es algo más que un comentario, también a Gerencia
            var roles = tipo.esAlerta() ? List.of(Role.SUPERVISOR, Role.GERENCIA) : List.of(Role.SUPERVISOR);
            notificacionService.notificarRoles(roles, TipoNotificacion.INCIDENCIA, mensaje, obra.getId(), tipo.tono(), actor);
        } else {
            // Si escribe el supervisor, el aviso es para el técnico de la obra
            notificacionService.notificar(obra.getTecnico(), TipoNotificacion.INCIDENCIA, mensaje, obra.getId(), tipo.tono());
        }
    }

    /** HU-24: el Técnico solo opera sobre las obras que tiene asignadas. */
    private Obra buscarObra(Long obraId, Usuario actor) {
        var obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
        boolean esTecnico = actor.getRole() == Role.TECNICO;
        boolean esSuya = obra.getTecnico() != null && obra.getTecnico().getId().equals(actor.getId());
        if (esTecnico && !esSuya) {
            throw new AccessDeniedException("Solo puede operar sobre las obras que tiene asignadas.");
        }
        return obra;
    }
}
