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

        // Registra el presupuesto con los datos validados.
        var creado = presupuestoService.registrar(datos);
        // Construye la URI del presupuesto creado.
        var uri = uriComponentsBuilder.path("/presupuestos/{id}").buildAndExpand(creado.id()).toUri();
        // Responde con estado 201 y el detalle del presupuesto.
        return ResponseEntity.created(uri).body(creado);
    }

    /** Listar presupuestos con montos (?estado=PENDIENTE). Finanzas y Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @GetMapping
    public ResponseEntity<PagedModel<ListaPresupuestoDTO>> listar(
            @RequestParam(required = false) EstadoPresupuesto estado,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable paginacion) {

        // Consulta los presupuestos aplicando estado y paginación.
        var presupuestos = presupuestoService.listar(estado, paginacion);
        // Devuelve el resultado paginado.
        return ResponseEntity.ok(new PagedModel<>(presupuestos));
    }

    /** Presupuestos aprobados SIN montos, para que el Supervisor cree obras. */
    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/aprobados")
    public ResponseEntity<List<ResumenPresupuestoDTO>> aprobados() {
        // Obtiene los presupuestos aprobados disponibles para crear obras.
        var presupuestos = presupuestoService.aprobados();
        // Devuelve la lista sin exponer montos.
        return ResponseEntity.ok(presupuestos);
    }

    /** Detalle con ítems y montos. Finanzas y Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @GetMapping("/{id}")
    public ResponseEntity<DetallesPresupuestoDTO> detalle(@PathVariable Long id) {
        // Busca el presupuesto solicitado por su identificador.
        var presupuesto = presupuestoService.obtener(id);
        // Devuelve el detalle con sus ítems y montos.
        return ResponseEntity.ok(presupuesto);
    }

    /** Modificar ítems mientras esté PENDIENTE. Solo Finanzas. */
    @PreAuthorize("hasRole('FINANZAS')")
    @PutMapping("/{id}")
    public ResponseEntity<DetallesPresupuestoDTO> actualizar(
            @PathVariable Long id,
            @RequestBody @Valid ModificarPresupuestoDTO datos) {

        // Actualiza los ítems del presupuesto pendiente.
        var presupuesto = presupuestoService.actualizar(id, datos);
        // Devuelve el presupuesto actualizado.
        return ResponseEntity.ok(presupuesto);
    }

    /** Aprobar un presupuesto PENDIENTE. Sobre el umbral, solo Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<DetallesPresupuestoDTO> aprobar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario actor) {

        // Solicita la aprobación del presupuesto con el usuario actual.
        var presupuesto = presupuestoService.aprobar(id, actor);
        // Devuelve el presupuesto con su nuevo estado.
        return ResponseEntity.ok(presupuesto);
    }

    /** Rechazar un presupuesto PENDIENTE con motivo obligatorio. Sobre el umbral, solo Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<DetallesPresupuestoDTO> rechazar(
            @PathVariable Long id,
            @RequestBody @Valid RechazarPresupuestoDTO datos,
            @AuthenticationPrincipal Usuario actor) {

        // Solicita el rechazo del presupuesto con el motivo indicado.
        var presupuesto = presupuestoService.rechazar(id, datos, actor);
        // Devuelve el presupuesto con su nuevo estado.
        return ResponseEntity.ok(presupuesto);
    }

    /** HU-27: descarga el PDF institucional de un presupuesto aprobado. Finanzas y Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        // Genera el PDF institucional del presupuesto.
        var contenido = presupuestoPdfService.generar(id);
        // Configura la descarga con un nombre y tipo de contenido adecuados.
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("presupuesto-" + id + ".pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenido);
    }
}