package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.incidencia.IncidenciaService;
import com.inacap.fyagobras.domain.incidencia.dto.DetallesIncidenciaDTO;
import com.inacap.fyagobras.domain.incidencia.dto.RegistrarIncidenciaDTO;
import com.inacap.fyagobras.domain.user.Usuario;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/obras/{obraId}/incidencias")
public class IncidenciaController {

    @Autowired
    private IncidenciaService incidenciaService;

    /** Dejar una novedad en la obra. Técnico (en sus obras) y Supervisor. */
    @PreAuthorize("hasAnyRole('TECNICO','SUPERVISOR')")
    @PostMapping
    public ResponseEntity<DetallesIncidenciaDTO> registrar(
            @PathVariable Long obraId,
            @RequestBody @Valid RegistrarIncidenciaDTO datos,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidenciaService.registrar(obraId, datos, actor));
    }

    /** Historial de novedades. El Técnico solo en sus obras. */
    @PreAuthorize("hasAnyRole('SUPERVISOR','GERENCIA','FINANZAS','ADMIN','TECNICO')")
    @GetMapping
    public ResponseEntity<List<DetallesIncidenciaDTO>> listar(
            @PathVariable Long obraId,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(incidenciaService.listar(obraId, actor));
    }
}