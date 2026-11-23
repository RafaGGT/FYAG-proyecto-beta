package com.inacap.fyagobras.domain.notificacion;

import com.inacap.fyagobras.domain.notificacion.dto.NotificacionDTO;
import com.inacap.fyagobras.domain.notificacion.dto.ResumenNotificacionesDTO;
import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import com.inacap.fyagobras.domain.user.repository.UsuarioRepository;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.inacap.fyagobras.domain.notificacion.repository.NotificacionRepository;
import java.util.Collection;

/**
 * Crea y consulta avisos. Los demás módulos llaman a notificar(...) dentro de su propia transacción:
 * si el cambio de negocio se revierte, el aviso tampoco queda guardado.
 */
@Service
// Clase NotificacionService: implementa la funcionalidad del dominio.
public class NotificacionService {

    private static final int MAX_RESUMEN = 30;
    private static final int MAX_MENSAJE = 300;

    @Autowired private NotificacionRepository repository;
    @Autowired private UsuarioRepository usuarioRepository;

    /** Avisa a un usuario. Si el destinatario es null (obra sin técnico, por ejemplo) no hace nada. */
    @Transactional
    // Ejecuta la operacion notificar.
    public void notificar(Usuario destinatario, TipoNotificacion tipo, String mensaje, Long obraId, String tono) {
        if (destinatario == null) return;
        repository.save(new Notificacion(destinatario.getId(), obraId, tipo, recortar(mensaje), tono));
    }

    /** Avisa a todos los usuarios activos de esos perfiles, menos a quien ejecutó la acción. */
    @Transactional
    // Ejecuta la operacion notificarRoles.
    public void notificarRoles(Collection<Role> roles, TipoNotificacion tipo, String mensaje,
                               Long obraId, String tono, Usuario excluir) {
        for (var rol : roles) {
            usuarioRepository.findByRoleAndActivoTrueOrderByApellidoAsc(rol).stream()
                    .filter(u -> excluir == null || !u.getId().equals(excluir.getId()))
                    .forEach(u -> notificar(u, tipo, mensaje, obraId, tono));
        }
    }

    @Transactional(readOnly = true)
    // Ejecuta la operacion resumen.
    public ResumenNotificacionesDTO resumen(Usuario actor) {
        var items = repository
                .findByDestinatarioIdOrderByFechaCreacionDesc(actor.getId(), PageRequest.of(0, MAX_RESUMEN))
                .stream().map(NotificacionDTO::new).toList();
        return new ResumenNotificacionesDTO(repository.countByDestinatarioIdAndLeidaFalse(actor.getId()), items);
    }

    @Transactional
    // Ejecuta la operacion marcarLeida.
    public void marcarLeida(Long id, Usuario actor) {
        var n = repository.findByIdAndDestinatarioId(id, actor.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificación no encontrada"));
        n.marcarLeida();
    }

    @Transactional
    // Ejecuta la operacion marcarTodasLeidas.
    public void marcarTodasLeidas(Usuario actor) {
        repository.marcarTodasLeidas(actor.getId());
    }

    // Ejecuta la operacion recortar.
    private static String recortar(String texto) {
        return texto.length() <= MAX_MENSAJE ? texto : texto.substring(0, MAX_MENSAJE - 1) + "…";
    }
}
