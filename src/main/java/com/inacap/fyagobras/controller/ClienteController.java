package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.cliente.ClienteService;
import com.inacap.fyagobras.domain.cliente.dto.DetallesClienteDTO;
import com.inacap.fyagobras.domain.cliente.dto.ModificarClienteDTO;
import com.inacap.fyagobras.domain.cliente.dto.RegistrarClienteDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    /** Registrar un cliente. Solo Finanzas. */
    @PreAuthorize("hasRole('FINANZAS')")
    @PostMapping
    public ResponseEntity<DetallesClienteDTO> registrar(
            @RequestBody @Valid RegistrarClienteDTO datos,
            UriComponentsBuilder uriComponentsBuilder) {

        var creado = clienteService.registrar(datos);
        var uri = uriComponentsBuilder.path("/clientes/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(uri).body(creado);
    }

    /** Listar o buscar clientes por RUT o razón social (?busqueda=texto). */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA','SUPERVISOR')")
    @GetMapping
    public ResponseEntity<PagedModel<DetallesClienteDTO>> listar(
            @RequestParam(required = false) String busqueda,
            @PageableDefault(size = 20, sort = "razonSocial") Pageable paginacion) {

        return ResponseEntity.ok(new PagedModel<>(clienteService.listar(busqueda, paginacion)));
    }

    /** Ver el detalle de un cliente. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA','SUPERVISOR')")
    @GetMapping("/{id}")
    public ResponseEntity<DetallesClienteDTO> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtener(id));
    }

    /** Editar datos de contacto. El RUT no se puede cambiar. Solo Finanzas. */
    @PreAuthorize("hasRole('FINANZAS')")
    @PutMapping("/{id}")
    public ResponseEntity<DetallesClienteDTO> actualizar(
            @PathVariable Long id,
            @RequestBody @Valid ModificarClienteDTO datos) {

        return ResponseEntity.ok(clienteService.actualizar(id, datos));
    }
}