package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.factura.EstadoPago;
import com.inacap.fyagobras.domain.factura.FacturaService;
import com.inacap.fyagobras.domain.factura.dto.*;
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

@RestController
@RequestMapping("/facturas")
public class FacturaController {

    @Autowired
    private FacturaService facturaService;

    /** Emitir factura sobre una obra finalizada. Solo Finanzas. */
    @PreAuthorize("hasRole('FINANZAS')")
    @PostMapping
    public ResponseEntity<DetallesFacturaDTO> emitir(
            @RequestBody @Valid EmitirFacturaDTO datos,
            @AuthenticationPrincipal Usuario actor,
            UriComponentsBuilder uriComponentsBuilder) {

        var creada = facturaService.emitir(datos, actor);
        var uri = uriComponentsBuilder.path("/facturas/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(uri).body(creada);
    }

    /** Listar facturas (?estadoPago=PENDIENTE). Finanzas y Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @GetMapping
    public ResponseEntity<PagedModel<DetallesFacturaDTO>> listar(
            @RequestParam(required = false) EstadoPago estadoPago,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable paginacion) {

        return ResponseEntity.ok(new PagedModel<>(facturaService.listar(estadoPago, paginacion)));
    }

    /** Detalle de una factura. Finanzas y Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @GetMapping("/{id}")
    public ResponseEntity<DetallesFacturaDTO> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.obtener(id));
    }

    /** Registrar el pago de una factura pendiente. Solo Finanzas. */
    @PreAuthorize("hasRole('FINANZAS')")
    @PatchMapping("/{id}/pago")
    public ResponseEntity<DetallesFacturaDTO> registrarPago(
            @PathVariable Long id,
            @RequestBody @Valid RegistrarPagoDTO datos,
            @AuthenticationPrincipal Usuario actor) {

        return ResponseEntity.ok(facturaService.registrarPago(id, datos, actor));
    }

    /** Anular un pago registrado por error (nota de crédito interna). Solo Finanzas. */
    @PreAuthorize("hasRole('FINANZAS')")
    @PatchMapping("/{id}/anular-pago")
    public ResponseEntity<DetallesFacturaDTO> anularPago(
            @PathVariable Long id,
            @RequestBody @Valid AnularPagoDTO datos,
            @AuthenticationPrincipal Usuario actor) {

        return ResponseEntity.ok(facturaService.anularPago(id, datos, actor));
    }

    /** HU-15: clientes con pagos pendientes (?busqueda=RUT o razón social). Finanzas y Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @GetMapping("/clientes-pendientes")
    public ResponseEntity<List<ClientePendienteDTO>> clientesPendientes(
            @RequestParam(required = false) String busqueda) {

        return ResponseEntity.ok(facturaService.clientesConPagosPendientes(busqueda));
    }

}