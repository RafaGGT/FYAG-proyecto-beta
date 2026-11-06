package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.factura.EstadoPago;
import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.reporte.ArchivoReporte;
import com.inacap.fyagobras.domain.reporte.FormatoReporte;
import com.inacap.fyagobras.domain.reporte.ReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/reportes")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    /** HU-19 y HU-20: reporte de obras (PDF o Excel). Finanzas y Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @GetMapping("/obras")
    public ResponseEntity<byte[]> obras(
            @RequestParam FormatoReporte formato,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(required = false) EstadoObra estado) {

        return descarga(reporteService.obras(formato, desde, hasta, clienteId, tecnicoId, estado));
    }

    /** HU-19: reporte de cobranzas (PDF o Excel). Finanzas y Gerencia. */
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    @GetMapping("/cobranzas")
    public ResponseEntity<byte[]> cobranzas(
            @RequestParam FormatoReporte formato,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) EstadoPago estadoPago) {

        return descarga(reporteService.cobranzas(formato, desde, hasta, clienteId, estadoPago));
    }

    private ResponseEntity<byte[]> descarga(ArchivoReporte archivo) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombreArchivo()).build().toString())
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .body(archivo.contenido());
    }
}