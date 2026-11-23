package com.inacap.fyagobras.domain.reporte;

// Registro ArchivoReporte: representa datos inmutables.
public record ArchivoReporte(byte[] contenido, String nombreArchivo, String contentType) {
}
