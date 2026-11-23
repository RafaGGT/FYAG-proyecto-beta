package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.notificacion.NotificacionService;
import com.inacap.fyagobras.domain.notificacion.dto.ResumenNotificacionesDTO;
import com.inacap.fyagobras.domain.user.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Cada usuario solo accede a sus propios avisos: el filtro es por el usuario autenticado, no por un id de la URL. */
@RestController
@RequestMapping("/notificaciones")
// Este controlador administra únicamente las notificaciones del usuario que inició sesión.
// No recibe un identificador ajeno en la URL; usa @AuthenticationPrincipal para encontrar al usuario.
public class NotificacionController {

    // Servicio que obtiene avisos y cambia su estado de lectura.
    @Autowired
    private NotificacionService notificacionService;

    /** Contador de no leídas y las 30 más recientes. Todos los perfiles. */
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<ResumenNotificacionesDTO> resumen(@AuthenticationPrincipal Usuario actor) {
        // El servicio cuenta avisos no leídos y devuelve los recientes.
        // 200 OK entrega el resumen dentro del cuerpo JSON.
        return ResponseEntity.ok(notificacionService.resumen(actor));
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        // El ID identifica la notificación y actor confirma que pertenece al usuario autenticado.
        // Void significa que no se enviará ningún dato en el cuerpo.
        // 204 No Content confirma que la operación terminó sin información adicional.
        notificacionService.marcarLeida(id, actor);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/leidas")
    public ResponseEntity<Void> marcarTodasLeidas(@AuthenticationPrincipal Usuario actor) {
        // El servicio marca todos los avisos del usuario como leídos.
        // 204 No Content confirma el cambio sin devolver un objeto.
        notificacionService.marcarTodasLeidas(actor);
        return ResponseEntity.noContent().build();
    }
}