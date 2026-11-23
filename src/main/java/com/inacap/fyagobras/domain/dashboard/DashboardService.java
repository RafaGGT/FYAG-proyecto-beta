package com.inacap.fyagobras.domain.dashboard;

import com.inacap.fyagobras.domain.dashboard.dto.DashboardDTO;
import com.inacap.fyagobras.domain.factura.repository.FacturaRepository;
import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.obra.repository.ObraRepository;
import com.inacap.fyagobras.domain.presupuesto.EstadoPresupuesto;
import com.inacap.fyagobras.domain.presupuesto.repository.PresupuestoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

// Calcula y consolida los indicadores generales mostrados en el dashboard.
@Service
// Clase DashboardService: implementa la funcionalidad del dominio.
public class DashboardService {

    @Autowired private FacturaRepository facturaRepository;
    @Autowired private PresupuestoRepository presupuestoRepository;
    @Autowired private ObraRepository obraRepository;

    /** HU-16: los cuatro indicadores del panel, calculados al momento de la consulta. */
    @Transactional(readOnly = true)
    // Ejecuta la operacion obtener.
    public DashboardDTO obtener() {
        // Determina el mes actual usando la zona horaria del negocio.
        var mes = YearMonth.now(ZoneId.of("America/Santiago"));
        // Calcula el primer y último día del mes para filtrar las facturas emitidas.
        LocalDate desde = mes.atDay(1);
        // Calcula el último día del mes para filtrar las facturas emitidas.
        LocalDate hasta = mes.atEndOfMonth();

        // Consulta cada indicador y construye una respuesta única para el panel.
        return new DashboardDTO(
            // Suma de las facturas emitidas en el mes actual.
                facturaRepository.sumaEmitidaEntre(desde, hasta),
                // Cantidad de presupuestos aprobados y pendientes.
                presupuestoRepository.countByEstado(EstadoPresupuesto.APROBADO),
                // Cantidad de presupuestos pendientes de aprobación.
                presupuestoRepository.countByEstado(EstadoPresupuesto.PENDIENTE),
                // Cantidad de obras en proceso y suma de facturas por cobrar.
                obraRepository.countByEstado(EstadoObra.EN_PROCESO),
                // Suma de las facturas pendientes de cobro, es decir, aquellas que aún no han sido pagadas.
                facturaRepository.sumaPorCobrar());
    }
}
