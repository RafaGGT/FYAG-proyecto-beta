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
// Este controlador permite registrar novedades y consultar su historial.
// Cada operación trabaja sobre la obra cuyo ID aparece en la URL.
public class IncidenciaController {

    // Servicio que valida permisos y persiste las incidencias.
    @Autowired
    private IncidenciaService incidenciaService;

    /** Dejar una novedad en la obra. Técnico (en sus obras) y Supervisor. */
    @PreAuthorize("hasAnyRole('TECNICO','SUPERVISOR')")
    @PostMapping
    public ResponseEntity<DetallesIncidenciaDTO> registrar(
            @PathVariable Long obraId,
            @RequestBody @Valid RegistrarIncidenciaDTO datos,
            @AuthenticationPrincipal Usuario actor) {
        // @PathVariable identifica la obra; @RequestBody contiene la novedad; actor identifica quién la informa.
        // El servicio valida permisos y crea la incidencia.
        // 201 Created indica que se creó un nuevo recurso y el cuerpo entrega el registro creado.
        return ResponseEntity.status(HttpStatus.CREATED).body(incidenciaService.registrar(obraId, datos, actor));
    }

    /** Historial de novedades. El Técnico solo en sus obras. */
    @PreAuthorize("hasAnyRole('SUPERVISOR','GERENCIA','FINANZAS','ADMIN','TECNICO')")
    @GetMapping
    public ResponseEntity<List<DetallesIncidenciaDTO>> listar(
            @PathVariable Long obraId,
            @AuthenticationPrincipal Usuario actor) {
        // El servicio obtiene el historial y también valida si el técnico puede ver esa obra.
        // 200 OK indica que la consulta fue realizada; el cuerpo es una lista, que puede estar vacía.
        return ResponseEntity.ok(incidenciaService.listar(obraId, actor));
    }
}