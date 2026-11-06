package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.presupuesto.EstadoPresupuesto;
import com.inacap.fyagobras.domain.presupuesto.PresupuestoPdfService;
import com.inacap.fyagobras.domain.presupuesto.PresupuestoService;
import com.inacap.fyagobras.domain.presupuesto.dto.*;
import com.inacap.fyagobras.domain.user.Usuario;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

// Define la clase como un controlador REST y mapea la ruta base "/presupuestos"
@RestController
@RequestMapping("/presupuestos")
public class PresupuestoController {

    // Inyecta el servicio que contiene la lógica de negocio para los presupuestos
    @Autowired
    private PresupuestoService presupuestoService;

    // Inyecta el servicio que genera el PDF institucional (HU-27)
    @Autowired
    private PresupuestoPdfService presupuestoPdfService;

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

    /** Aprobar un presupuesto PENDIENTE. Sobre el umbral, solo Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<DetallesPresupuestoDTO> aprobar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario actor) {

        return ResponseEntity.ok(presupuestoService.aprobar(id, actor));
    }

    /** Rechazar un presupuesto PENDIENTE con motivo obligatorio. Sobre el umbral, solo Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<DetallesPresupuestoDTO> rechazar(
            @PathVariable Long id,
            @RequestBody @Valid RechazarPresupuestoDTO datos,
            @AuthenticationPrincipal Usuario actor) {

        return ResponseEntity.ok(presupuestoService.rechazar(id, datos, actor));
    }

    /** HU-27: descarga el PDF institucional de un presupuesto aprobado. Finanzas y Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        var contenido = presupuestoPdfService.generar(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("presupuesto-" + id + ".pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenido);
    }
}