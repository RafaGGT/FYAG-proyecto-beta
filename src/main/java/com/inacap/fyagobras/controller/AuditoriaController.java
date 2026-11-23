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
// Este controlador recibe consultas de auditoría y las convierte en respuestas HTTP.
// ADMIN y GERENCIA pueden consultar todos los métodos; cada usuario autenticado no define este permiso.
public class AuditoriaController {

    // AuditoriaService realiza las búsquedas y entrega las páginas de registros.
    // Se conecta con la base MongoDB sin que este controlador necesite conocer la consulta interna.
    @Autowired
    private AuditoriaService auditoriaService;

    /** Ver todos los registros (20 por página, más recientes primero). */
    @GetMapping
    public ResponseEntity<PagedModel<DetallesAuditoriaDTO>> listar(
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable paginacion) {
        // @PageableDefault crea la página predeterminada: 20 registros, desde la fecha más reciente.
        // Page contiene los registros y metadatos como número de página y cantidad total.
        // PagedModel transforma esa información para que Spring la entregue como JSON.
        // El estado HTTP 200 OK indica que la consulta fue exitosa.
        return ResponseEntity.ok(new PagedModel<>(auditoriaService.listar(paginacion)));
    }

    /** Ver los registros de un usuario: /auditoria/usuario/7 */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<PagedModel<DetallesAuditoriaDTO>> porUsuario(
            @PathVariable Long usuarioId,
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable paginacion) {
        // @PathVariable toma el valor usuarioId desde la URL, por ejemplo /auditoria/usuario/7.
        // El servicio filtra los registros para ese usuario y devuelve una página paginada.
        // 200 OK representa una búsqueda exitosa; una página vacía también sería una respuesta válida.
        return ResponseEntity.ok(new PagedModel<>(auditoriaService.listarPorUsuario(usuarioId, paginacion)));
    }

    /** Ver los registros por fecha: /auditoria/fecha?desde=2026-10-01&hasta=2026-10-20 */
    @GetMapping("/fecha")
    public ResponseEntity<PagedModel<DetallesAuditoriaDTO>> porFecha(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = "fecha", direction = Sort.Direction.DESC) Pageable paginacion) {
        // @RequestParam recibe desde y hasta como parámetros de consulta, por ejemplo ?desde=2026-10-01.
        // @DateTimeFormat permite interpretar el texto enviado como una fecha LocalDate válida.
        // ResponseEntity devuelve 200 OK con una PagedModel cuando el rango fue procesado correctamente.
        return ResponseEntity.ok(new PagedModel<>(auditoriaService.listarPorFecha(desde, hasta, paginacion)));
    }
}