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
import com.inacap.fyagobras.domain.obra.dto.AsignarTecnicoDTO;
import com.inacap.fyagobras.domain.notificacion.NotificacionService;
import com.inacap.fyagobras.domain.notificacion.TipoNotificacion;
import java.util.List;
@RestController
@RequestMapping("/obras")
// Este controlador recibe solicitudes relacionadas con la creación y el ciclo de vida de las obras.
// Los métodos hacen poco trabajo directo: validan entrada y delegan las reglas complejas al ObraService.
public class ObraController {

    // Servicios utilizados para gestionar obras, facturas y notificaciones.
    @Autowired
    private ObraService obraService;
    @Autowired
    private FacturaRepository facturaRepository;
    @Autowired
    private NotificacionService notificacionService;

    /** Crear obra desde un presupuesto aprobado. Solo Supervisor. */
    @PreAuthorize("hasRole('SUPERVISOR')")
    @PostMapping
    public ResponseEntity<DetallesObraDTO> registrar(
            @RequestBody @Valid RegistrarObraDTO datos,
            @AuthenticationPrincipal Usuario actor,
            UriComponentsBuilder uriComponentsBuilder) {

        // @RequestBody convierte el JSON en RegistrarObraDTO y @Valid activa sus reglas.
        // actor registra quién creó la obra y UriComponentsBuilder construye su URL.
        // Registra la obra usando los datos y el usuario autenticado.
        var creada = obraService.registrar(datos, actor);
        // Construye la URI de la obra creada.
        var uri = uriComponentsBuilder.path("/obras/{id}").buildAndExpand(creada.id()).toUri();
        // 201 Created indica que el recurso fue creado y Ubicación apunta a la nueva obra.
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

    /** Asignar o cambiar el técnico responsable. Solo Supervisor. */
    @PreAuthorize("hasRole('SUPERVISOR')")
    @PatchMapping("/{id}/tecnico")
    public ResponseEntity<DetallesObraDTO> asignarTecnico(
            @PathVariable Long id,
            @RequestBody @Valid AsignarTecnicoDTO datos,
            @AuthenticationPrincipal Usuario actor) {
        // Solicita el cambio de técnico responsable
        var obra = obraService.asignarTecnico(id, datos, actor);
        // Devuelve la obra con el técnico actualizado
        return ResponseEntity.ok(obra);
    }

}
        // estado es opcional y puede filtrar obras por su fase.
        // actor permite que un técnico reciba solamente sus obras.
        // Pageable controla tamaño de página y orden; PagedModel presenta la información paginada.
        // 200 OK indica que la consulta fue realizada correctamente.
        // El ID identifica la obra y actor permite verificar si el técnico puede verla.
        // 200 OK entrega el detalle; los errores de autorización o inexistencia los controla el servicio.
        // @Valid comprueba el nuevo estado recibido antes de delegar.
        // 200 OK entrega la obra después de aplicar las reglas de transición.
        // El cuerpo indica qué técnico debe quedar asignado.
        // 200 OK entrega la obra con su nuevo técnico responsable.