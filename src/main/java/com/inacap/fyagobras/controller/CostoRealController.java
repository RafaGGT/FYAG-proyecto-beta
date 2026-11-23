package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.costo.CostoRealService;
import com.inacap.fyagobras.domain.costo.dto.DetallesCostosDTO;
import com.inacap.fyagobras.domain.costo.dto.RegistrarCostosDTO;
import com.inacap.fyagobras.domain.costo.dto.RentabilidadDTO;
import com.inacap.fyagobras.domain.user.Usuario;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/obras/{obraId}")
// Este controlador administra las horas técnicas y materiales usados por una obra.
// ResponseEntity permite indicar si existe información, si fue guardada o si no hay costos todavía.
public class CostoRealController {

    // Servicio que aplica las reglas de materiales, horas técnicas y rentabilidad.
    @Autowired
    private CostoRealService costoService;

    /** Registrar o reemplazar horas y materiales de la obra. Supervisor, o Técnico en sus obras. */
    @PreAuthorize("hasAnyRole('SUPERVISOR','TECNICO')")
    @PutMapping("/costos")
    public ResponseEntity<DetallesCostosDTO> registrar(
            @PathVariable Long obraId,
            @RequestBody @Valid RegistrarCostosDTO datos,
            @AuthenticationPrincipal Usuario actor) {

        // obraId identifica la obra dentro de la URL.
        // datos contiene las horas y materiales enviados en el cuerpo JSON.
        // actor identifica al usuario autenticado para validar permisos y registrar auditoría.
        // Registra o reemplaza los costos de la obra.
        var costos = costoService.registrar(obraId, datos, actor);
        // 200 OK indica que se guardaron los costos y se devuelven sus datos actualizados.
        return ResponseEntity.ok(costos);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR','TECNICO')")
    @GetMapping("/costos")
    public ResponseEntity<DetallesCostosDTO> obtener(
            @PathVariable Long obraId,
            @AuthenticationPrincipal Usuario actor) {

        // Si la obra aún no tiene costos, responde 204 (sin contenido) en vez de un error 404.
        // map() envía el detalle con 200 OK cuando Optional contiene datos.
        // orElseGet() crea 204 No Content cuando Optional está vacío.
        return costoService.obtener(obraId, actor)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Presupuesto aprobado vs costo real. Solo Gerencia y Finanzas. */
    @PreAuthorize("hasAnyRole('GERENCIA','FINANZAS')")
    @GetMapping("/rentabilidad")
    public ResponseEntity<RentabilidadDTO> rentabilidad(@PathVariable Long obraId) {
        // Sin costos registrados responde 204 No Content en vez de 404 Recurso no encontrado.
        // Con datos disponibles responde 200 OK y entrega el presupuesto y costo comparados.
        return costoService.rentabilidad(obraId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }


}