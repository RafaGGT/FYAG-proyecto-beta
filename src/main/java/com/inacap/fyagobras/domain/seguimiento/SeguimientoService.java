package com.inacap.fyagobras.domain.seguimiento;

import com.inacap.fyagobras.domain.costo.repository.CostoRealRepository;
import com.inacap.fyagobras.domain.factura.EstadoPago;
import com.inacap.fyagobras.domain.factura.Factura;
import com.inacap.fyagobras.domain.factura.repository.FacturaRepository;
import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.obra.Obra;
import com.inacap.fyagobras.domain.obra.ObraService;
import com.inacap.fyagobras.domain.obra.repository.ObraRepository;
import com.inacap.fyagobras.domain.presupuesto.EstadoPresupuesto;
import com.inacap.fyagobras.domain.presupuesto.Presupuesto;
import com.inacap.fyagobras.domain.presupuesto.repository.PresupuestoRepository;
import com.inacap.fyagobras.domain.seguimiento.dto.PasoSeguimientoDTO;
import com.inacap.fyagobras.domain.seguimiento.dto.SeguimientoDTO;
import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Seguimiento tipo "rastreo de paquete": recorre el ciclo presupuesto, obra, factura y pago
 * y dice en qué paso está y a quién espera. No guarda nada: todo se calcula con los datos que ya existen.
 */
@Service
public class SeguimientoService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Autowired private PresupuestoRepository presupuestoRepository;
    @Autowired private ObraRepository obraRepository;
    @Autowired private ObraService obraService;
    @Autowired private CostoRealRepository costoRepository;
    @Autowired private FacturaRepository facturaRepository;

    @Value("${app.presupuesto.umbral-aprobacion}")
    private BigDecimal umbralAprobacion;

    /** Un paso candidato: cómo se ve cuando ya pasó y cómo cuando el proceso lo está esperando. */
    private record Paso(String titulo, String tituloEspera, String responsable, boolean hecho,
                        LocalDate fecha, String detalleHecho, String detalleEspera) {
    }

    /** Seguimiento de un presupuesto (incluye la obra y la cobranza si ya existen). Finanzas y Gerencia. */
    @Transactional(readOnly = true)
    public SeguimientoDTO porPresupuesto(Long presupuestoId, Usuario actor) {
        var presupuesto = presupuestoRepository.findById(presupuestoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Presupuesto no encontrado"));
        var obra = obraRepository.findByPresupuestoId(presupuestoId).orElse(null);
        return construir(presupuesto, obra, actor);
    }

    /** Seguimiento de una obra. Reutiliza la validación de acceso: el Técnico solo ve las suyas. */
    @Transactional(readOnly = true)
    public SeguimientoDTO porObra(Long obraId, Usuario actor) {
        obraService.obtener(obraId, actor);
        var obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
        return construir(obra.getPresupuesto(), obra, actor);
    }

    private SeguimientoDTO construir(Presupuesto presupuesto, Obra obra, Usuario actor) {
        var creado = new Paso("Presupuesto creado", null, "Finanzas", true,
                presupuesto.getFechaCreacion(), presupuesto.getNumeroPresupuesto(), null);

        // Un presupuesto rechazado termina el recorrido ahí
        if (presupuesto.getEstado() == EstadoPresupuesto.RECHAZADO) {
            var pasos = List.of(
                    new PasoSeguimientoDTO(creado.titulo(), creado.detalleHecho(), null, creado.fecha(), EstadoPaso.COMPLETADO),
                    new PasoSeguimientoDTO("Presupuesto rechazado", "Motivo: " + presupuesto.getMotivoRechazo(),
                            null, null, EstadoPaso.ERROR));
            return new SeguimientoDTO("Presupuesto rechazado", pasos);
        }

        boolean verFacturacion = actor.getRole() == Role.FINANZAS || actor.getRole() == Role.GERENCIA;
        // Sobre el umbral decide solo Gerencia; bajo el umbral, Finanzas
        var quienAprueba = presupuesto.getMontoTotal().compareTo(umbralAprobacion) > 0 ? "Gerencia" : "Finanzas";
        boolean hayObra = obra != null;
        boolean hayCostos = hayObra && costoRepository.existsByObraId(obra.getId());
        List<Factura> facturas = hayObra
                ? facturaRepository.findByObraIdAndAnuladaFalseOrderByIdAsc(obra.getId())
                : List.of();

        var candidatos = new ArrayList<Paso>();
        candidatos.add(creado);
        candidatos.add(new Paso("Presupuesto aprobado", "Esperando aprobación de " + quienAprueba, quienAprueba,
                presupuesto.getEstado() == EstadoPresupuesto.APROBADO, presupuesto.getFechaAprobacion(),
                null, quienAprueba.equals("Gerencia") ? "El monto supera el umbral: solo Gerencia puede decidir." : null));
        candidatos.add(new Paso("Obra creada", "Esperando creación de la obra por el Supervisor", "Supervisor",
                hayObra, hayObra ? obra.getFechaInicio() : null,
                hayObra ? obra.getNombre() : null, "Se crea desde el presupuesto aprobado."));
        candidatos.add(new Paso("Técnico asignado", "Esperando asignación de técnico", "Supervisor",
                hayObra && obra.getTecnico() != null, null,
                hayObra && obra.getTecnico() != null ? obra.getTecnico().getNombre() + " " + obra.getTecnico().getApellido() : null,
                null));
        candidatos.add(new Paso("Horas y materiales registrados", "Esperando horas y materiales del técnico", "Técnico",
                hayCostos, null, null, "Sin ellos la obra no puede finalizarse."));
        candidatos.add(new Paso("Obra finalizada", "Esperando que el Supervisor finalice la obra", "Supervisor",
                hayObra && obra.getEstado() == EstadoObra.FINALIZADA, hayObra ? obra.getFechaTermino() : null,
                null, null));

        if (verFacturacion) {
            candidatos.add(new Paso("Factura emitida", "Esperando emisión de factura por Finanzas", "Finanzas",
                    !facturas.isEmpty(), facturas.isEmpty() ? null : facturas.get(0).getFechaEmision(),
                    detalleFacturas(facturas), null));
            candidatos.add(new Paso("Pago recibido", "Esperando pago del cliente", "Cliente",
                    !facturas.isEmpty() && facturas.stream().allMatch(f -> f.getEstadoPago() == EstadoPago.PAGADO),
                    facturas.stream().map(Factura::getFechaPago).filter(f -> f != null).max(Comparator.naturalOrder()).orElse(null),
                    null, detalleVencimiento(facturas)));
        }

        // Una obra cancelada corta el recorrido en el último paso cumplido
        boolean cancelada = hayObra && obra.getEstado() == EstadoObra.CANCELADA;

        var salida = new ArrayList<PasoSeguimientoDTO>();
        boolean actualAsignado = false;
        String resumen = null;
        for (var c : candidatos) {
            if (c.hecho()) {
                salida.add(new PasoSeguimientoDTO(c.titulo(), c.detalleHecho(), null, c.fecha(), EstadoPaso.COMPLETADO));
            } else if (cancelada) {
                break;
            } else if (!actualAsignado) {
                actualAsignado = true;
                var paso = pasoActual(c, obra);
                salida.add(paso);
                resumen = paso.titulo();
            } else {
                salida.add(new PasoSeguimientoDTO(c.titulo(), null, c.responsable(), null, EstadoPaso.PENDIENTE));
            }
        }
        if (cancelada) {
            salida.add(new PasoSeguimientoDTO("Obra cancelada", "El recorrido termina aquí.", null, null, EstadoPaso.ERROR));
            resumen = "Obra cancelada";
        }
        if (resumen == null) {
            resumen = salida.get(salida.size() - 1).titulo(); // todo completado: se muestra el último paso
        }
        return new SeguimientoDTO(resumen, salida);
    }

    /** Si la obra está suspendida, el paso que se espera pasa a mostrarse como pausa con su motivo. */
    private PasoSeguimientoDTO pasoActual(Paso c, Obra obra) {
        if (obra != null && obra.getEstado() == EstadoObra.SUSPENDIDA) {
            return new PasoSeguimientoDTO("Obra suspendida",
                    "Motivo: " + obra.getMotivoSuspension() + ". Esperando que el Supervisor la reanude.",
                    "Supervisor", null, EstadoPaso.PAUSADO);
        }
        return new PasoSeguimientoDTO(c.tituloEspera(), c.detalleEspera(), c.responsable(), null, EstadoPaso.ACTUAL);
    }

    private String detalleFacturas(List<Factura> facturas) {
        if (facturas.isEmpty()) return null;
        if (facturas.size() == 1) {
            var f = facturas.get(0);
            return "N° " + f.getNumeroFactura() + " · vence " + f.getFechaVencimiento().format(FECHA);
        }
        return facturas.size() + " facturas emitidas";
    }

    /** Del pago pendiente más antiguo: cuándo vence o cuántos días lleva vencido. */
    private String detalleVencimiento(List<Factura> facturas) {
        var vencimiento = facturas.stream()
                .filter(f -> f.getEstadoPago() == EstadoPago.PENDIENTE)
                .map(Factura::getFechaVencimiento)
                .min(Comparator.naturalOrder());
        if (vencimiento.isEmpty()) return null;
        long atraso = ChronoUnit.DAYS.between(vencimiento.get(), LocalDate.now());
        return atraso > 0
                ? "Vencida hace " + atraso + " día(s)"
                : "Vence el " + vencimiento.get().format(FECHA);
    }
}
