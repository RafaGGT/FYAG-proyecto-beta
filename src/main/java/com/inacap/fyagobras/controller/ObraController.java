package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.factura.repository.FacturaRepository;
import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.obra.ObraService;
import com.inacap.fyagobras.domain.obra.dto.DetallesObraDTO;
import com.inacap.fyagobras.domain.obra.dto.RegistrarObraDTO;
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
import com.inacap.fyagobras.domain.obra.dto.CambiarEstadoObraDTO;

@RestController
@RequestMapping("/obras")
public class ObraController {

    @Autowired
    private ObraService obraService;
    @Autowired
    private FacturaRepository facturaRepository;

    /** Crear obra desde un presupuesto aprobado. Solo Supervisor. */
    @PreAuthorize("hasRole('SUPERVISOR')")
    @PostMapping
    public ResponseEntity<DetallesObraDTO> registrar(
            @RequestBody @Valid RegistrarObraDTO datos,
            @AuthenticationPrincipal Usuario actor,
            UriComponentsBuilder uriComponentsBuilder) {

        // Registra la obra usando los datos y el usuario autenticado.
        var creada = obraService.registrar(datos, actor);
        // Construye la URI de la obra creada.
        var uri = uriComponentsBuilder.path("/obras/{id}").buildAndExpand(creada.id()).toUri();
        // Responde con estado 201 y el detalle de la obra.
        return ResponseEntity.created(uri).body(creada);
    }

    /** Listar obras (?estado=EN_PROCESO). El Técnico ve solo las suyas. */
    @PreAuthorize("hasAnyRole('SUPERVISOR','GERENCIA','FINANZAS','ADMIN','TECNICO')")
    @GetMapping
    public ResponseEntity<PagedModel<DetallesObraDTO>> listar(
            @RequestParam(required = false) EstadoObra estado,
            @AuthenticationPrincipal Usuario actor,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable paginacion) {

        // Consulta las obras según estado, usuario y paginación.
        var obras = obraService.listar(estado, actor, paginacion);
        // Devuelve el resultado paginado.
        return ResponseEntity.ok(new PagedModel<>(obras));
    }

    /** Detalle de una obra. El Técnico solo si está asignada a él. */
    @PreAuthorize("hasAnyRole('SUPERVISOR','GERENCIA','FINANZAS','ADMIN','TECNICO')")
    @GetMapping("/{id}")
    public ResponseEntity<DetallesObraDTO> detalle(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario actor) {

        // Obtiene la obra validando el acceso del usuario actual.
        var obra = obraService.obtener(id, actor);
        // Devuelve el detalle de la obra.
        return ResponseEntity.ok(obra);
    }

    /** Cambiar el estado de la obra según la máquina de estados. Solo Supervisor. */
    @PreAuthorize("hasRole('SUPERVISOR')")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<DetallesObraDTO> cambiarEstado(
            @PathVariable Long id,
            @RequestBody @Valid CambiarEstadoObraDTO datos,
            @AuthenticationPrincipal Usuario actor) {

        // Solicita el cambio de estado de la obra.
        var obra = obraService.cambiarEstado(id, datos, actor);
        // Devuelve la obra con el nuevo estado.
        return ResponseEntity.ok(obra);
    }


}