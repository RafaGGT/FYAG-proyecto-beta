package com.inacap.fyagobras.domain.factura.validation.create;

import com.inacap.fyagobras.domain.factura.dto.EmitirFacturaDTO;

// Validador ValidarEmitirFactura: aplica una regla del dominio.
public interface ValidarEmitirFactura {
    // Ejecuta la operacion validate.
    void validate(EmitirFacturaDTO data);
}
