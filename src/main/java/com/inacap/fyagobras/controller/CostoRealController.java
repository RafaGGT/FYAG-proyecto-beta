package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.costo.CostoRealService;
import com.inacap.fyagobras.domain.costo.dto.DetallesCostosDTO;
import com.inacap.fyagobras.domain.costo.dto.RegistrarCostosDTO;
import com.inacap.fyagobras.domain.costo.dto.RentabilidadDTO;
import com.inacap.fyagobras.domain.user.Usuario;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/obras/{obraId}")
public class CostoRealController {

    @Autowired
    private CostoRealService costoService;

    /** Registrar o reemplazar horas y materiales de la obra. Supervisor, o Técnico en sus obras. */
    @PreAuthorize("hasAnyRole('SUPERVISOR','TECNICO')")
    @PutMapping("/costos")
    public ResponseEntity<DetallesCostosDTO> registrar(
            @PathVariable Long obraId,
            @RequestBody @Valid RegistrarCostosDTO datos,
            @AuthenticationPrincipal Usuario actor) {

        // Registra o reemplaza los costos de la obra.
        var costos = costoService.registrar(obraId, datos, actor);
        // Devuelve el resumen actualizado de costos.
        return ResponseEntity.ok(costos);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','TECNICO')")
    @GetMapping("/costos")
    public ResponseEntity<DetallesCostosDTO> obtener(
            @PathVariable Long obraId,
            @AuthenticationPrincipal Usuario actor) {

        // Si la obra aún no tiene costos, responde 204 (sin contenido) en vez de un error 404.
        return costoService.obtener(obraId, actor)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Presupuesto aprobado vs costo real. Solo Gerencia y Finanzas. */
    @PreAuthorize("hasAnyRole('GERENCIA','FINANZAS')")
    @GetMapping("/rentabilidad")
    public ResponseEntity<RentabilidadDTO> rentabilidad(@PathVariable Long obraId) {
        // Sin costos registrados responde 204 en vez de un error 404.
        return costoService.rentabilidad(obraId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }


}