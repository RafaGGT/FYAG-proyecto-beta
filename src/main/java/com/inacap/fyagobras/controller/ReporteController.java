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
// Este controlador recibe las peticiones HTTP relacionadas con reportes.
// Su trabajo es conectar una URL con el ReporteService y entregar el archivo generado.
public class ReporteController {

    // ReporteService consulta los datos y crea el archivo PDF o Excel solicitado.
    @Autowired
    private ReporteService reporteService;

    /** HU-19 y HU-20: reporte de obras (PDF o Excel). Finanzas y Gerencia. */
    // Solo permite acceder a usuarios con el rol FINANZAS o GERENCIA.
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    // Una petición GET a /reportes/obras ejecuta este método.
    @GetMapping("/obras")
    public ResponseEntity<byte[]> obras(
            // formato indica si el usuario desea recibir un reporte PDF o Excel.
            @RequestParam FormatoReporte formato,
            // desde y hasta representan las fechas inicial y final del periodo a consultar.
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            // clienteId permite mostrar solo las obras de un cliente específico.
            @RequestParam(required = false) Long clienteId,
            // tecnicoId permite mostrar solo las obras asignadas a un técnico.
            @RequestParam(required = false) Long tecnicoId,
            // estado permite filtrar obras por EN_PROCESO, FINALIZADA, SUSPENDIDA o CANCELADA.
            @RequestParam(required = false) EstadoObra estado) {

        // Paso 1: entrega todos los filtros al servicio para que consulte y genere el reporte.
        // El servicio devuelve un ArchivoReporte con el nombre, tipo y contenido del archivo.
        var archivo = reporteService.obras(formato, desde, hasta, clienteId, tecnicoId, estado);

        // Paso 2: convierte el resultado en una respuesta HTTP preparada para descarga.
        // ResponseEntity indica el estado HTTP, los encabezados y el contenido enviado al usuario.
        return descarga(archivo);
    }

    /** HU-19: reporte de cobranzas (PDF o Excel). Finanzas y Gerencia. */
    // Solo permite acceder a usuarios con el rol FINANZAS o GERENCIA.
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA')")
    // Una petición GET a /reportes/cobranzas ejecuta este método.
    @GetMapping("/cobranzas")
    public ResponseEntity<byte[]> cobranzas(
            // formato indica si se debe crear un archivo PDF o Excel.
            @RequestParam FormatoReporte formato,
            // Desde y hasta forman el rango opcional de fechas de la consulta.
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            // clienteId permite limitar las cobranzas a un único cliente.
            @RequestParam(required = false) Long clienteId,
            // estadoPago permite filtrar facturas pendientes, pagadas o anuladas.
            @RequestParam(required = false) EstadoPago estadoPago) {

        // Solicita al servicio que consulte las facturas y genere el archivo correspondiente.
        // El resultado contiene los bytes del archivo y la información necesaria para descargarlo.
        var archivo = reporteService.cobranzas(formato, desde, hasta, clienteId, estadoPago);

        // Entrega el archivo al navegador mediante ResponseEntity.
        return descarga(archivo);
    }

    // Convierte ArchivoReporte en una respuesta HTTP que el navegador pueda descargar.
    // ResponseEntity<byte[]> está formado por: estado HTTP, encabezados y cuerpo con bytes.
    private ResponseEntity<byte[]> descarga(ArchivoReporte archivo) {
        // Paso 1: crea el encabezado Content-Disposition.
        // "attachment" obliga al navegador a descargar el archivo en vez de abrirlo dentro de la página.
        // filename() establece el nombre que verá el usuario al guardar el archivo.
        var disposicion = ContentDisposition.attachment()
                .filename(archivo.nombreArchivo())
                .build()
                .toString();

        // Paso 2: construye la respuesta completa.
        // ok() establece el estado HTTP 200 OK, indicando que la operación fue exitosa.
        // header() agrega el nombre y la forma de descarga al encabezado HTTP.
        // contentType() indica el tipo real del contenido: application/pdf o Excel.
        // body() envía al usuario los bytes que representan el PDF o el archivo Excel.
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion)
                .contentType(MediaType.parseMediaType(archivo.contentType()))
                .body(archivo.contenido());
    }
}