package com.inacap.fyagobras.domain.factura.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

// Repositorio ClientePendienteProyeccion: define el acceso a datos.
public interface ClientePendienteProyeccion {
    // Ejecuta la operacion getClienteId.
    Long getClienteId();
    // Ejecuta la operacion getRut.
    String getRut();
    // Ejecuta la operacion getRazonSocial.
    String getRazonSocial();
    // Ejecuta la operacion getFacturasPendientes.
    Long getFacturasPendientes();
    // Ejecuta la operacion getMontoAdeudado.
    BigDecimal getMontoAdeudado();
    // Ejecuta la operacion getVencimientoMasAntiguo.
    LocalDate getVencimientoMasAntiguo();
}
