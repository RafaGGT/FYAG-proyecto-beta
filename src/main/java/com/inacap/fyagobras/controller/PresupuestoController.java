package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.presupuesto.EstadoPresupuesto;
import com.inacap.fyagobras.domain.presupuesto.PresupuestoService;
import com.inacap.fyagobras.domain.presupuesto.dto.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/presupuestos")
public class PresupuestoController {

    @Autowired
    private PresupuestoService presupuestoService;

    /** Registrar un presupuesto. Solo Finanzas. */
    @PreAuthorize("hasRole('FINANZAS')")
    @PostMapping
    public ResponseEntity<DetallesPresupuestoDTO> registrar(
            @RequestBody @Valid RegistrarPresupuestoDTO datos,
            UriComponentsBuilder uriComponentsBuilder) {

        var creado = presupuestoService.registrar(datos);
        var uri = uriComponentsBuilder.path("/presupuestos/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(uri).body(creado);
    }

    /** Listar presupuestos con montos (?estado=PENDIENTE). Finanzas y Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @GetMapping
    public ResponseEntity<PagedModel<ListaPresupuestoDTO>> listar(
            @RequestParam(required = false) EstadoPresupuesto estado,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable paginacion) {

        return ResponseEntity.ok(new PagedModel<>(presupuestoService.listar(estado, paginacion)));
    }

    /** Presupuestos aprobados SIN montos, para que el Supervisor cree obras. */
    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/aprobados")
    public ResponseEntity<List<ResumenPresupuestoDTO>> aprobados() {
        return ResponseEntity.ok(presupuestoService.aprobados());
    }

    /** Detalle con ítems y montos. Finanzas y Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @GetMapping("/{id}")
    public ResponseEntity<DetallesPresupuestoDTO> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(presupuestoService.obtener(id));
    }

    /** Modificar ítems mientras esté PENDIENTE. Solo Finanzas. */
    @PreAuthorize("hasRole('FINANZAS')")
    @PutMapping("/{id}")
    public ResponseEntity<DetallesPresupuestoDTO> actualizar(
            @PathVariable Long id,
            @RequestBody @Valid ModificarPresupuestoDTO datos) {

        return ResponseEntity.ok(presupuestoService.actualizar(id, datos));
    }
}