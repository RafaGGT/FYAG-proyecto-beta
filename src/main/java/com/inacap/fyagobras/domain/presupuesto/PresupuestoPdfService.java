package com.inacap.fyagobras.domain.presupuesto;

import com.inacap.fyagobras.domain.presupuesto.repository.PresupuestoRepository;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class PresupuestoPdfService {

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font NEGRITA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 11);

    @Autowired
    private PresupuestoRepository repository;

    /** HU-27: documento institucional con el logo de FYAG y los datos del presupuesto aprobado. */
    @Transactional(readOnly = true)
    public byte[] generar(Long id) {
        var presupuesto = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Presupuesto no encontrado"));

        if (presupuesto.getEstado() != EstadoPresupuesto.APROBADO) {
            throw new ConflictoNegocioException("El PDF solo está disponible para presupuestos APROBADOS.");
        }

        var moneda = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));

        try (var salida = new ByteArrayOutputStream()) {
            var documento = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(documento, salida);
            documento.open();

            agregarLogo(documento);

            var titulo = new Paragraph("Presupuesto N° " + presupuesto.getNumeroPresupuesto(), TITULO);
            titulo.setSpacingAfter(14);
            documento.add(titulo);

            documento.add(linea("Cliente: ", presupuesto.getCliente().getRazonSocial()));
            documento.add(linea("RUT: ", presupuesto.getCliente().getRut()));
            documento.add(linea("Fecha de aprobación: ", presupuesto.getFechaAprobacion().toString()));

            var tabla = new PdfPTable(new float[]{75f, 25f});
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(16);
            tabla.addCell(celda("Descripción", NEGRITA, Element.ALIGN_LEFT));
            tabla.addCell(celda("Monto", NEGRITA, Element.ALIGN_RIGHT));
            for (var item : presupuesto.getItems()) {
                tabla.addCell(celda(item.getDescripcion(), NORMAL, Element.ALIGN_LEFT));
                tabla.addCell(celda(moneda.format(item.getMonto()), NORMAL, Element.ALIGN_RIGHT));
            }
            tabla.addCell(celda("Total", NEGRITA, Element.ALIGN_RIGHT));
            tabla.addCell(celda(moneda.format(presupuesto.getMontoTotal()), NEGRITA, Element.ALIGN_RIGHT));
            documento.add(tabla);

            documento.close();
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF del presupuesto", e);
        }
    }

    private void agregarLogo(Document documento) throws Exception {
        var recurso = new ClassPathResource("static/logo-fyag.png");
        if (recurso.exists()) {
            var logo = Image.getInstance(recurso.getURL());
            logo.scaleToFit(140, 70);
            logo.setSpacingAfter(12);
            documento.add(logo);
        }
    }

    private Paragraph linea(String etiqueta, String valor) {
        var p = new Paragraph();
        p.add(new Phrase(etiqueta, NEGRITA));
        p.add(new Phrase(valor, NORMAL));
        return p;
    }

    private PdfPCell celda(String texto, Font fuente, int alineacion) {
        var c = new PdfPCell(new Phrase(texto, fuente));
        c.setHorizontalAlignment(alineacion);
        c.setPadding(6);
        return c;
    }
}