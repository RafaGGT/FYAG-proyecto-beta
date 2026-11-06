package com.inacap.fyagobras.domain.factura.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ClientePendienteProyeccion {
    Long getClienteId();
    String getRut();
    String getRazonSocial();
    Long getFacturasPendientes();
    BigDecimal getMontoAdeudado();
    LocalDate getVencimientoMasAntiguo();
}