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

        // Consulta los tipos de servicio según el filtro de vigencia.
        var tipos = tipoServicioService.listar(incluirInactivos);
        // Devuelve la lista para el cliente solicitante.
        return ResponseEntity.ok(tipos);
    }

    /** Crear un tipo de servicio. Solo Supervisor. */
    @PreAuthorize("hasRole('SUPERVISOR')")
    @PostMapping
    public ResponseEntity<DetallesTipoServicioDTO> registrar(
            @RequestBody @Valid RegistrarTipoServicioDTO datos,
            UriComponentsBuilder uriComponentsBuilder) {

        // Registra el nuevo tipo de servicio.
        var creado = tipoServicioService.registrar(datos);
        // Construye la URI del tipo de servicio creado.
        var uri = uriComponentsBuilder.path("/tipos-servicio/{id}").buildAndExpand(creado.id()).toUri();
        // Responde con estado 201 y el recurso creado.
        return ResponseEntity.created(uri).body(creado);
    }

    /** Editar el nombre. Solo Supervisor. */
    @PreAuthorize("hasRole('SUPERVISOR')")
    @PutMapping("/{id}")
    public ResponseEntity<DetallesTipoServicioDTO> modificar(
            @PathVariable Long id,
            @RequestBody @Valid ModificarTipoServicioDTO datos) {

        // Actualiza el nombre del tipo de servicio.
        var tipo = tipoServicioService.modificar(id, datos);
        // Devuelve el recurso actualizado.
        return ResponseEntity.ok(tipo);
    }

    /** Activar o desactivar (nunca se elimina). Solo Supervisor. */
    @PreAuthorize("hasRole('SUPERVISOR')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<DetallesTipoServicioDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestBody @Valid CambiarEstadoTipoServicioDTO datos) {

        // Cambia la vigencia del tipo de servicio sin eliminarlo.
        var tipo = tipoServicioService.cambiarEstado(id, datos);
        // Devuelve el recurso con su nuevo estado.
        return ResponseEntity.ok(tipo);
    }
}