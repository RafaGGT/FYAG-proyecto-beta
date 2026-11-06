package com.inacap.fyagobras.domain.reporte;

public record ArchivoReporte(byte[] contenido, String nombreArchivo, String contentType) {
}