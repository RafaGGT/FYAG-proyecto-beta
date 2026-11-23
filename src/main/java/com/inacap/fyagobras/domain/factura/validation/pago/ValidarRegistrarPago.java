package com.inacap.fyagobras.domain.factura.validation.pago;

import com.inacap.fyagobras.domain.factura.Factura;
import com.inacap.fyagobras.domain.factura.dto.RegistrarPagoDTO;

// Validador ValidarRegistrarPago: aplica una regla del dominio.
public interface ValidarRegistrarPago {
    // Ejecuta la operacion validate.
    void validate(Factura factura, RegistrarPagoDTO data);
}
