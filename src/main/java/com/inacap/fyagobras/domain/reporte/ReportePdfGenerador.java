package com.inacap.fyagobras.domain.reporte;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;

@Component
public class ReportePdfGenerador {

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 9);

    public byte[] generar(ReporteDatos datos) {
        var moneda = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));

        try (var salida = new ByteArrayOutputStream()) {
            var documento = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(documento, salida);
            documento.open();

            agregarLogo(documento);

            var titulo = new Paragraph(datos.titulo(), TITULO);
            titulo.setSpacingAfter(6);
            documento.add(titulo);
            documento.add(new Paragraph("Generado el " + LocalDate.now(), NORMAL));
            documento.add(new Paragraph(" ", NORMAL));

            documento.add(new Paragraph("Filtros aplicados", NEGRITA));
            for (var filtro : datos.filtros()) {
                documento.add(new Paragraph("• " + filtro, NORMAL));
            }

            var tabla = new PdfPTable(datos.columnas().size());
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(12);
            tabla.setHeaderRows(1);
            for (var columna : datos.columnas()) {
                var celda = celda(columna, NEGRITA, Element.ALIGN_LEFT);
                celda.setBackgroundColor(Color.LIGHT_GRAY);
                tabla.addCell(celda);
            }
            for (var fila : datos.filas()) {
                for (var valor : fila) {
                    if (valor instanceof BigDecimal monto) {
                        tabla.addCell(celda(moneda.format(monto), NORMAL, Element.ALIGN_RIGHT));
                    } else {
                        tabla.addCell(celda(String.valueOf(valor), NORMAL, Element.ALIGN_LEFT));
                    }
                }
            }
            documento.add(tabla);

            var resumenTitulo = new Paragraph("Resumen", NEGRITA);
            resumenTitulo.setSpacingBefore(12);
            documento.add(resumenTitulo);
            for (var linea : datos.resumen()) {
                documento.add(new Paragraph(linea, NORMAL));
            }

            documento.close();
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el reporte en PDF", e);
        }
    }

    private void agregarLogo(Document documento) throws Exception {
        var recurso = new ClassPathResource("static/logo-fyag.png");
        if (recurso.exists()) {
            var logo = Image.getInstance(recurso.getURL());
            logo.scaleToFit(110, 55);
            logo.setSpacingAfter(8);
            documento.add(logo);
        }
    }

    private PdfPCell celda(String texto, Font fuente, int alineacion) {
        var c = new PdfPCell(new Phrase(texto, fuente));
        c.setHorizontalAlignment(alineacion);
        c.setPadding(4);
        return c;
    }
}