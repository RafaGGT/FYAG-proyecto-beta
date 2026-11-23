package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.servicio.TipoServicioService;
import com.inacap.fyagobras.domain.servicio.dto.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import com.inacap.fyagobras.domain.user.Usuario;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/tipos-servicio")
// Este controlador administra los tipos de servicio disponibles para obras.
// Permite crear, modificar, listar y activar o desactivar servicios sin eliminarlos físicamente.
public class TipoServicioController {

    // Servicio que valida y mantiene activos o inactivos los tipos de servicio.
    @Autowired
    private TipoServicioService tipoServicioService;

    /** Lista para el desplegable (solo vigentes) o completa con ?incluirInactivos=true. No Técnico. */
    @PreAuthorize("hasAnyRole('SUPERVISOR','GERENCIA','FINANZAS','ADMIN')")
    @GetMapping
    public ResponseEntity<List<DetallesTipoServicioDTO>> listar(
            @RequestParam(defaultValue = "false") boolean incluirInactivos) {

        // false lista solo servicios vigentes; true solicita incluir los desactivados.
        // 200 OK entrega una lista, aunque esta pueda estar vacía.
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
            @AuthenticationPrincipal Usuario actor, UriComponentsBuilder uriComponentsBuilder) {

        // @Valid valida los datos antes de llamar al servicio.
        // UriComponentsBuilder crea una dirección HTTP apuntando al nuevo recurso.
        // Registra el nuevo tipo de servicio.
        var creado = tipoServicioService.registrar(datos, actor);
        // Construye la URI del tipo de servicio creado.
        var uri = uriComponentsBuilder.path("/tipos-servicio/{id}").buildAndExpand(creado.id()).toUri();
        // 201 Created indica que se creó el recurso y Ubicación señala su URI.
        return ResponseEntity.created(uri).body(creado);
    }

    /** Editar el nombre. Solo Supervisor. */
    @PreAuthorize("hasRole('SUPERVISOR')")
    @PutMapping("/{id}")
    public ResponseEntity<DetallesTipoServicioDTO> modificar(
            // 200 OK entrega el tipo actualizado dentro del cuerpo JSON.
            @PathVariable Long id,
            @RequestBody @Valid ModificarTipoServicioDTO datos, @AuthenticationPrincipal Usuario actor) {

        // Actualiza el nombre del tipo de servicio.
        var tipo = tipoServicioService.modificar(id, datos, actor);
        // Devuelve el recurso actualizado.
        return ResponseEntity.ok(tipo);
    }

    /** Activar o desactivar (nunca se elimina). Solo Supervisor. */
    @PreAuthorize("hasRole('SUPERVISOR')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<DetallesTipoServicioDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestBody @Valid CambiarEstadoTipoServicioDTO datos, @AuthenticationPrincipal Usuario actor) {

        // Cambia la vigencia del tipo de servicio sin eliminarlo.
        var tipo = tipoServicioService.cambiarEstado(id, datos, actor);
        // Devuelve el recurso con su nuevo estado.
        return ResponseEntity.ok(tipo);
    }
}
            // 200 OK indica que el cambio de vigencia fue aplicado.