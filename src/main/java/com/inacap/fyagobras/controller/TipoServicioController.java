package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.servicio.TipoServicioService;
import com.inacap.fyagobras.domain.servicio.dto.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/tipos-servicio")
public class TipoServicioController {

    @Autowired
    private TipoServicioService tipoServicioService;

    /** Lista para el desplegable (solo vigentes) o completa con ?incluirInactivos=true. No Técnico. */
    @PreAuthorize("hasAnyRole('SUPERVISOR','GERENCIA','FINANZAS','ADMIN')")
    @GetMapping
    public ResponseEntity<List<DetallesTipoServicioDTO>> listar(
            @RequestParam(defaultValue = "false") boolean incluirInactivos) {

        return ResponseEntity.ok(tipoServicioService.listar(incluirInactivos));
    }

    /** Crear un tipo de servicio. Solo Supervisor. */
    @PreAuthorize("hasRole('SUPERVISOR')")
    @PostMapping
    public ResponseEntity<DetallesTipoServicioDTO> registrar(
            @RequestBody @Valid RegistrarTipoServicioDTO datos,
            UriComponentsBuilder uriComponentsBuilder) {

        var creado = tipoServicioService.registrar(datos);
        var uri = uriComponentsBuilder.path("/tipos-servicio/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(uri).body(creado);
    }

    /** Editar el nombre. Solo Supervisor. */
    @PreAuthorize("hasRole('SUPERVISOR')")
    @PutMapping("/{id}")
    public ResponseEntity<DetallesTipoServicioDTO> modificar(
            @PathVariable Long id,
            @RequestBody @Valid ModificarTipoServicioDTO datos) {

        return ResponseEntity.ok(tipoServicioService.modificar(id, datos));
    }

    /** Activar o desactivar (nunca se elimina). Solo Supervisor. */
    @PreAuthorize("hasRole('SUPERVISOR')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<DetallesTipoServicioDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestBody @Valid CambiarEstadoTipoServicioDTO datos) {

        return ResponseEntity.ok(tipoServicioService.cambiarEstado(id, datos));
    }
}