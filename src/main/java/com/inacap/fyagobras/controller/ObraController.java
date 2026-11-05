package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.obra.ObraService;
import com.inacap.fyagobras.domain.obra.dto.DetallesObraDTO;
import com.inacap.fyagobras.domain.obra.dto.RegistrarObraDTO;
import com.inacap.fyagobras.domain.user.Usuario;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import com.inacap.fyagobras.domain.obra.dto.CambiarEstadoObraDTO;

@RestController
@RequestMapping("/obras")
public class ObraController {

    @Autowired
    private ObraService obraService;

    /** Crear obra desde un presupuesto aprobado. Solo Supervisor. */
    @PreAuthorize("hasRole('SUPERVISOR')")
    @PostMapping
    public ResponseEntity<DetallesObraDTO> registrar(
            @RequestBody @Valid RegistrarObraDTO datos,
            @AuthenticationPrincipal Usuario actor,
            UriComponentsBuilder uriComponentsBuilder) {

        var creada = obraService.registrar(datos, actor);
        var uri = uriComponentsBuilder.path("/obras/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(uri).body(creada);
    }

    /** Listar obras (?estado=EN_PROCESO). El Técnico ve solo las suyas. */
    @PreAuthorize("hasAnyRole('SUPERVISOR','GERENCIA','FINANZAS','ADMIN','TECNICO')")
    @GetMapping
    public ResponseEntity<PagedModel<DetallesObraDTO>> listar(
            @RequestParam(required = false) EstadoObra estado,
            @AuthenticationPrincipal Usuario actor,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable paginacion) {

        return ResponseEntity.ok(new PagedModel<>(obraService.listar(estado, actor, paginacion)));
    }

    /** Detalle de una obra. El Técnico solo si está asignada a él. */
    @PreAuthorize("hasAnyRole('SUPERVISOR','GERENCIA','FINANZAS','ADMIN','TECNICO')")
    @GetMapping("/{id}")
    public ResponseEntity<DetallesObraDTO> detalle(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario actor) {

        return ResponseEntity.ok(obraService.obtener(id, actor));
    }

    /** Cambiar el estado de la obra según la máquina de estados. Solo Supervisor. */
    @PreAuthorize("hasRole('SUPERVISOR')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<DetallesObraDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestBody @Valid CambiarEstadoObraDTO datos,
            @AuthenticationPrincipal Usuario actor) {

        return ResponseEntity.ok(obraService.cambiarEstado(id, datos, actor));
    }


}