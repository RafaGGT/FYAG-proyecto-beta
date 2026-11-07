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

        // Envía los datos al servicio para emitir la factura.
        var creada = facturaService.emitir(datos, actor);
        // Construye la URI del recurso recién creado.
        var uri = uriComponentsBuilder.path("/facturas/{id}").buildAndExpand(creada.id()).toUri();
        // Responde con estado 201 y los datos de la factura.
        return ResponseEntity.created(uri).body(creada);
    }

    /** Listar facturas (?estadoPago=PENDIENTE). Finanzas y Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @GetMapping
    public ResponseEntity<PagedModel<DetallesFacturaDTO>> listar(
            @RequestParam(required = false) EstadoPago estadoPago,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable paginacion) {

        // Consulta las facturas aplicando el filtro y la paginación.
        var facturas = facturaService.listar(estadoPago, paginacion);
        // Envuelve el resultado paginado y responde con estado 200.
        return ResponseEntity.ok(new PagedModel<>(facturas));
    }

    /** Detalle de una factura. Finanzas y Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @GetMapping("/{id}")
    public ResponseEntity<DetallesFacturaDTO> detalle(@PathVariable Long id) {
        // Obtiene la factura solicitada desde el servicio.
        var factura = facturaService.obtener(id);
        // Devuelve el detalle encontrado.
        return ResponseEntity.ok(factura);
    }

    /** Registrar el pago de una factura pendiente. Solo Finanzas. */
    @PreAuthorize("hasRole('FINANZAS')")
    @PatchMapping("/{id}/pago")
    public ResponseEntity<DetallesFacturaDTO> registrarPago(
            @PathVariable Long id,
            @RequestBody @Valid RegistrarPagoDTO datos,
            @AuthenticationPrincipal Usuario actor) {

        // Registra el pago asociado a la factura.
        var factura = facturaService.registrarPago(id, datos, actor);
        // Devuelve la factura con su estado actualizado.
        return ResponseEntity.ok(factura);
    }

    /** Anular un pago registrado por error (nota de crédito interna). Solo Finanzas. */
    @PreAuthorize("hasRole('FINANZAS')")
    @PatchMapping("/{id}/anular-pago")
    public ResponseEntity<DetallesFacturaDTO> anularPago(
            @PathVariable Long id,
            @RequestBody @Valid AnularPagoDTO datos,
            @AuthenticationPrincipal Usuario actor) {

        // Solicita al servicio la anulación del pago.
        var factura = facturaService.anularPago(id, datos, actor);
        // Devuelve la factura actualizada.
        return ResponseEntity.ok(factura);
    }

    /** HU-15: clientes con pagos pendientes (?busqueda=RUT o razón social). Finanzas y Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @GetMapping("/clientes-pendientes")
    public ResponseEntity<List<ClientePendienteDTO>> clientesPendientes(
            @RequestParam(required = false) String busqueda) {

        // Busca los clientes que mantienen pagos pendientes.
        var clientes = facturaService.clientesConPagosPendientes(busqueda);
        // Devuelve la lista resultante.
        return ResponseEntity.ok(clientes);
    }

}