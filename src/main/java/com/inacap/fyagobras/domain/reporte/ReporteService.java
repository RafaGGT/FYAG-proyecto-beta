package com.inacap.fyagobras.domain.reporte;

import com.inacap.fyagobras.domain.cliente.Cliente;
import com.inacap.fyagobras.domain.cliente.repository.ClienteRepository;
import com.inacap.fyagobras.domain.factura.EstadoPago;
import com.inacap.fyagobras.domain.factura.repository.FacturaRepository;
import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.obra.repository.ObraRepository;
import com.inacap.fyagobras.domain.user.Usuario;
import com.inacap.fyagobras.domain.user.repository.UsuarioRepository;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ReporteService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final String CONTENT_TYPE_EXCEL =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired private ObraRepository obraRepository;
    @Autowired private FacturaRepository facturaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ReportePdfGenerador pdfGenerador;
    @Autowired private ReporteExcelGenerador excelGenerador;

    /** HU-19 y HU-20: reporte de obras con filtros por fecha de inicio, cliente, técnico y estado. */
    @Transactional(readOnly = true)
    public ArchivoReporte obras(FormatoReporte formato, LocalDate desde, LocalDate hasta,
                                Long clienteId, Long tecnicoId, EstadoObra estado) {
        validarRango(desde, hasta);

        var filtros = new ArrayList<String>();
        if (desde != null) filtros.add("Inicio desde: " + FECHA.format(desde));
        if (hasta != null) filtros.add("Inicio hasta: " + FECHA.format(hasta));
        if (clienteId != null) filtros.add("Cliente: " + cliente(clienteId).getRazonSocial());
        if (tecnicoId != null) {
            var t = tecnico(tecnicoId);
            filtros.add("Técnico: " + t.getNombre() + " " + t.getApellido());
        }
        if (estado != null) filtros.add("Estado: " + estado);
        if (filtros.isEmpty()) filtros.add("Sin filtros (todas las obras)");

        var obras = obraRepository.findAll(
                ReporteSpecs.obras(desde, hasta, clienteId, tecnicoId, estado), Sort.by("id"));

        var filas = new ArrayList<List<Object>>();
        for (var o : obras) {
            filas.add(List.<Object>of(
                    o.getNombre(),
                    o.getPresupuesto().getCliente().getRazonSocial(),
                    o.getTipoServicio().getNombre(),
                    o.getTecnico() == null ? "Sin asignar"
                            : o.getTecnico().getNombre() + " " + o.getTecnico().getApellido(),
                    o.getEstado().name(),
                    fecha(o.getFechaInicio()),
                    fecha(o.getFechaTermino())));
        }

        var resumen = new ArrayList<String>();
        resumen.add("Total de obras: " + obras.size());
        for (var e : EstadoObra.values()) {
            long cantidad = obras.stream().filter(o -> o.getEstado() == e).count();
            if (cantidad > 0) resumen.add(e + ": " + cantidad);
        }

        var datos = new ReporteDatos("Reporte de obras", filtros,
                List.of("Obra", "Cliente", "Tipo de servicio", "Técnico", "Estado", "Inicio", "Término"),
                filas, resumen);
        return empaquetar(datos, formato, "reporte-obras");
    }

    /** HU-19: reporte de cobranzas con filtros por fecha de emisión, cliente y estado de pago. */
    @Transactional(readOnly = true)
    public ArchivoReporte cobranzas(FormatoReporte formato, LocalDate desde, LocalDate hasta,
                                    Long clienteId, EstadoPago estadoPago) {
        validarRango(desde, hasta);
        var moneda = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));

        var filtros = new ArrayList<String>();
        if (desde != null) filtros.add("Emisión desde: " + FECHA.format(desde));
        if (hasta != null) filtros.add("Emisión hasta: " + FECHA.format(hasta));
        if (clienteId != null) filtros.add("Cliente: " + cliente(clienteId).getRazonSocial());
        if (estadoPago != null) filtros.add("Estado de pago: " + estadoPago);
        if (filtros.isEmpty()) filtros.add("Sin filtros (todas las facturas)");

        var facturas = facturaRepository.findAll(
                ReporteSpecs.facturas(desde, hasta, clienteId, estadoPago), Sort.by("id"));

        var filas = new ArrayList<List<Object>>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal pendiente = BigDecimal.ZERO;
        for (var f : facturas) {
            filas.add(List.<Object>of(
                    f.getNumeroFactura(),
                    f.getObra().getPresupuesto().getCliente().getRazonSocial(),
                    f.getObra().getNombre(),
                    fecha(f.getFechaEmision()),
                    fecha(f.getFechaVencimiento()),
                    fecha(f.getFechaPago()),
                    f.getMonto(),
                    f.getEstadoPago().name()));
            total = total.add(f.getMonto());
            if (f.getEstadoPago() == EstadoPago.PENDIENTE) {
                pendiente = pendiente.add(f.getMonto());
            }
        }

        var resumen = List.of(
                "Total de facturas: " + facturas.size(),
                "Monto total facturado: " + moneda.format(total),
                "Monto pendiente de cobro: " + moneda.format(pendiente),
                "Monto pagado: " + moneda.format(total.subtract(pendiente)));

        var datos = new ReporteDatos("Reporte de cobranzas", filtros,
                List.of("N° factura", "Cliente", "Obra", "Emisión", "Vencimiento", "Pago", "Monto", "Estado de pago"),
                filas, resumen);
        return empaquetar(datos, formato, "reporte-cobranzas");
    }

    private ArchivoReporte empaquetar(ReporteDatos datos, FormatoReporte formato, String base) {
        var hoy = LocalDate.now();
        return switch (formato) {
            case PDF -> new ArchivoReporte(pdfGenerador.generar(datos),
                    base + "-" + hoy + ".pdf", "application/pdf");
            case EXCEL -> new ArchivoReporte(excelGenerador.generar(datos),
                    base + "-" + hoy + ".xlsx", CONTENT_TYPE_EXCEL);
        };
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ConflictoNegocioException("La fecha 'desde' no puede ser posterior a la fecha 'hasta'.");
        }
    }

    private Cliente cliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
    }

    private Usuario tecnico(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Técnico no encontrado"));
    }

    private String fecha(LocalDate fecha) {
        return fecha == null ? "-" : FECHA.format(fecha);
    }
}