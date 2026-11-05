package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.costo.dto.DetallesCostosDTO;
import com.inacap.fyagobras.domain.presupuesto.EstadoPresupuesto;
import com.inacap.fyagobras.domain.presupuesto.PresupuestoService;
import com.inacap.fyagobras.domain.presupuesto.dto.*;
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

import java.util.List;

// Define la clase como un controlador REST y mapea la ruta base "/presupuestos"
@RestController
@RequestMapping("/presupuestos")
public class PresupuestoController {

    // Inyecta el servicio que contiene la lógica de negocio para los presupuestos
    @Autowired
    private PresupuestoService presupuestoService;

    /** Registrar un presupuesto. Solo Finanzas. */
    // Restringe el acceso exclusivo al rol FINANZAS
    @PreAuthorize("hasRole('FINANZAS')")
    // Define un endpoint POST para registrar un nuevo presupuesto
    @PostMapping
    public ResponseEntity<DetallesPresupuestoDTO> registrar(
            @RequestBody @Valid RegistrarPresupuestoDTO datos,
            UriComponentsBuilder uriComponentsBuilder) {

        // Llama al servicio para registrar el presupuesto y obtiene sus detalles
        var creado = presupuestoService.registrar(datos);
        // Construye la URI del recurso recién creado (/presupuestos/{id})
        var uri = uriComponentsBuilder.path("/presupuestos/{id}").buildAndExpand(creado.id()).toUri();
        // Retorna una respuesta 201 Created con la URI y el cuerpo con los detalles del presupuesto
        return ResponseEntity.created(uri).body(creado);
    }

    /** Listar presupuestos con montos (?estado=PENDIENTE). Finanzas y Gerencia. */
    // Restringe el acceso a los roles FINANZAS o GERENCIA
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    // Define un endpoint GET para listar presupuestos de forma paginada y filtrable por estado
    @GetMapping
    public ResponseEntity<PagedModel<ListaPresupuestoDTO>> listar(
            @RequestParam(required = false) EstadoPresupuesto estado,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable paginacion) {

        // Retorna la página de presupuestos envuelta en un PagedModel con estado 200 OK
        return ResponseEntity.ok(new PagedModel<>(presupuestoService.listar(estado, paginacion)));
    }

    /** Presupuestos aprobados SIN montos, para que el Supervisor cree obras. */
    // Restringe el acceso exclusivo al rol SUPERVISOR
    @PreAuthorize("hasRole('SUPERVISOR')")
    // Define un endpoint GET en "/presupuestos/aprobados" para listar los presupuestos aprobados
    @GetMapping("/aprobados")
    public ResponseEntity<List<ResumenPresupuestoDTO>> aprobados() {
        // Retorna la lista de presupuestos aprobados sin montos con estado 200 OK
        return ResponseEntity.ok(presupuestoService.aprobados());
    }

    /** Detalle con ítems y montos. Finanzas y Gerencia. */
    // Restringe el acceso a los roles FINANZAS o GERENCIA
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    // Define un endpoint GET para consultar los detalles completos de un presupuesto por su ID
    @GetMapping("/{id}")
    public ResponseEntity<DetallesPresupuestoDTO> detalle(@PathVariable Long id) {
        // Retorna los detalles del presupuesto obtenido con estado 200 OK
        return ResponseEntity.ok(presupuestoService.obtener(id));
    }

    /** Modificar ítems mientras esté PENDIENTE. Solo Finanzas. */
    // Restringe el acceso exclusivo al rol FINANZAS
    @PreAuthorize("hasRole('FINANZAS')")
    // Define un endpoint PUT para actualizar un presupuesto existente por su ID
    @PutMapping("/{id}")
    public ResponseEntity<DetallesPresupuestoDTO> actualizar(
            @PathVariable Long id,
            @RequestBody @Valid ModificarPresupuestoDTO datos) {

        // Actualiza el presupuesto a través del servicio y retorna el resultado con estado 200 OK
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

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/costos")
    public ResponseEntity<DetallesCostosDTO> obtener(
            @PathVariable Long obraId,
            @AuthenticationPrincipal Usuario actor) {
        return ResponseEntity.ok(costoService.obtener(obraId, actor));
    }
}