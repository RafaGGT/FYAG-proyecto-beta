package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.seguimiento.SeguimientoService;
import com.inacap.fyagobras.domain.seguimiento.dto.SeguimientoDTO;
import com.inacap.fyagobras.domain.user.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
// Este controlador muestra el paso actual de presupuestos y obras.
// El usuario autenticado permite aplicar restricciones adicionales al resultado.
public class SeguimientoController {

    // Servicio que calcula el flujo de seguimiento de cada proceso.
    @Autowired
    private SeguimientoService seguimientoService;

    /** En qué paso va un presupuesto y quién debe actuar. Finanzas y Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @GetMapping("/presupuestos/{id}/seguimiento")
    public ResponseEntity<SeguimientoDTO> presupuesto(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario actor) {
        // @PathVariable identifica el presupuesto y actor indica quién solicita la información.
        // El servicio calcula qué pasó, qué sigue y quién debe actuar.
        // 200 OK devuelve el seguimiento completo en formato JSON.
        return ResponseEntity.ok(seguimientoService.porPresupuesto(id, actor));
    }

    /** En qué paso va una obra. El Técnico solo ve las suyas; facturación solo la ven Finanzas y Gerencia. */
    @PreAuthorize("hasAnyRole('SUPERVISOR','GERENCIA','FINANZAS','ADMIN','TECNICO')")
    @GetMapping("/obras/{id}/seguimiento")
    public ResponseEntity<SeguimientoDTO> obra(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario actor) {
        // El servicio valida si el técnico tiene permiso para consultar esta obra.
        // 200 OK indica que el seguimiento fue encontrado y puede entregarse.
        return ResponseEntity.ok(seguimientoService.porObra(id, actor));
    }
}
