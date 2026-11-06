package com.inacap.fyagobras.domain.reporte;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

@Component
public class ReporteExcelGenerador {

    public byte[] generar(ReporteDatos datos) {
        try (var libro = new XSSFWorkbook(); var salida = new ByteArrayOutputStream()) {
            var hoja = libro.createSheet("Reporte");

            var negrita = libro.createFont();
            negrita.setBold(true);

            var estiloNegrita = libro.createCellStyle();
            estiloNegrita.setFont(negrita);

            var estiloEncabezado = libro.createCellStyle();
            estiloEncabezado.setFont(negrita);
            estiloEncabezado.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            var estiloMonto = libro.createCellStyle();
            estiloMonto.setDataFormat(libro.createDataFormat().getFormat("#,##0"));

            int fila = 0;

            var titulo = hoja.createRow(fila++).createCell(0);
            titulo.setCellValue(datos.titulo());
            titulo.setCellStyle(estiloNegrita);

            hoja.createRow(fila++).createCell(0).setCellValue("Filtros aplicados:");
            for (var filtro : datos.filtros()) {
                hoja.createRow(fila++).createCell(0).setCellValue(filtro);
            }
            fila++;

            var encabezado = hoja.createRow(fila++);
            for (int i = 0; i < datos.columnas().size(); i++) {
                var celda = encabezado.createCell(i);
                celda.setCellValue(datos.columnas().get(i));
                celda.setCellStyle(estiloEncabezado);
            }

            for (var datosFila : datos.filas()) {
                var row = hoja.createRow(fila++);
                for (int i = 0; i < datosFila.size(); i++) {
                    var celda = row.createCell(i);
                    var valor = datosFila.get(i);
                    if (valor instanceof BigDecimal monto) {
                        celda.setCellValue(monto.doubleValue());
                        celda.setCellStyle(estiloMonto);
                    } else {
                        celda.setCellValue(String.valueOf(valor));
                    }
                }
            }
            fila++;

            var resumen = hoja.createRow(fila++).createCell(0);
            resumen.setCellValue("Resumen");
            resumen.setCellStyle(estiloNegrita);
            for (var linea : datos.resumen()) {
                hoja.createRow(fila++).createCell(0).setCellValue(linea);
            }

            // Ancho fijo: autoSizeColumn necesita fuentes del sistema y falla en contenedores Alpine
            for (int i = 0; i < datos.columnas().size(); i++) {
                hoja.setColumnWidth(i, 22 * 256);
            }

            libro.write(salida);
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el reporte en Excel", e);
        }
    }
}