package com.inacap.fyagobras.domain.factura.dto;

import com.inacap.fyagobras.domain.factura.EstadoPago;
import com.inacap.fyagobras.domain.factura.Factura;

import java.math.BigDecimal;
import java.time.LocalDate;

// DTO DetallesFacturaDTO: transporta datos entre el dominio y la API.
public record DetallesFacturaDTO(
        Long id,
        String numeroFactura,
        Long obraId,
        String obraNombre,
        String clienteRazonSocial,
        LocalDate fechaEmision,
        LocalDate fechaVencimiento,
        LocalDate fechaPago,
        BigDecimal monto,
        EstadoPago estadoPago,
        boolean anulada,
        String motivoAnulacion
) {
    // Ejecuta la operacion DetallesFacturaDTO.
    public DetallesFacturaDTO(Factura f) {
        this(f.getId(), f.getNumeroFactura(),
                f.getObra().getId(), f.getObra().getNombre(),
                f.getObra().getPresupuesto().getCliente().getRazonSocial(),
                f.getFechaEmision(), f.getFechaVencimiento(), f.getFechaPago(),
                f.getMonto(), f.getEstadoPago(), f.isAnulada(), f.getMotivoAnulacion());
    }
}
