package com.inacap.fyagobras.domain.reporte;

import java.util.List;

/** Contenido neutro del reporte: el mismo modelo se exporta a PDF o a Excel. */
public record ReporteDatos(
        String titulo,
        List<String> filtros,
        List<String> columnas,
        List<List<Object>> filas,
        List<String> resumen
) {
}