package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.audit.dto.DetallesAuditoriaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/auditoria")
@PreAuthorize("hasAnyRole('ADMIN','GERENCIA')")
public class AuditoriaController {

    @Autowired
    private AuditoriaService auditoriaService;

    /** Ver todos los registros (20 por página, más recientes primero). */
    @GetMapping
    public ResponseEntity<PagedModel<DetallesAuditoriaDTO>> listar(
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable paginacion) {
        return ResponseEntity.ok(new PagedModel<>(auditoriaService.listar(paginacion)));
    }

    /** Ver los registros de un usuario: /auditoria/usuario/7 */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<PagedModel<DetallesAuditoriaDTO>> porUsuario(
            @PathVariable Long usuarioId,
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable paginacion) {
        return ResponseEntity.ok(new PagedModel<>(auditoriaService.listarPorUsuario(usuarioId, paginacion)));
    }

    /** Ver los registros por fecha: /auditoria/fecha?desde=2026-10-01&hasta=2026-10-20 */
    @GetMapping("/fecha")
    public ResponseEntity<PagedModel<DetallesAuditoriaDTO>> porFecha(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable paginacion) {
        return ResponseEntity.ok(new PagedModel<>(auditoriaService.listarPorFecha(desde, hasta, paginacion)));
    }
}