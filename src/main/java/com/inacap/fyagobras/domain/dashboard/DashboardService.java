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

@Service
public class DashboardService {

    @Autowired private FacturaRepository facturaRepository;
    @Autowired private PresupuestoRepository presupuestoRepository;
    @Autowired private ObraRepository obraRepository;

    /** HU-16: los cuatro indicadores del panel, calculados al momento de la consulta. */
    @Transactional(readOnly = true)
    public DashboardDTO obtener() {
        // Determina el mes actual usando la zona horaria del negocio.
        var mes = YearMonth.now(ZoneId.of("America/Santiago"));
        LocalDate desde = mes.atDay(1);
        LocalDate hasta = mes.atEndOfMonth();

        // Consulta cada indicador y construye una respuesta única para el panel.
        return new DashboardDTO(
                facturaRepository.sumaEmitidaEntre(desde, hasta),
                presupuestoRepository.countByEstado(EstadoPresupuesto.APROBADO),
                presupuestoRepository.countByEstado(EstadoPresupuesto.PENDIENTE),
                obraRepository.countByEstado(EstadoObra.EN_PROCESO),
                facturaRepository.sumaPorCobrar());
    }
}