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
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    /** Contador de no leídas y las 30 más recientes. Todos los perfiles. */
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<ResumenNotificacionesDTO> resumen(@AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(notificacionService.resumen(actor));
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id, @AuthenticationPrincipal Usuario actor) {
        notificacionService.marcarLeida(id, actor);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/leidas")
    public ResponseEntity<Void> marcarTodasLeidas(@AuthenticationPrincipal Usuario actor) {
        notificacionService.marcarTodasLeidas(actor);
        return ResponseEntity.noContent().build();
    }
}