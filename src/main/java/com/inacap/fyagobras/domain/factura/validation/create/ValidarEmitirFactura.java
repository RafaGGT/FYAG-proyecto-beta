package com.inacap.fyagobras.domain.factura.validation.create;

import com.inacap.fyagobras.domain.factura.dto.EmitirFacturaDTO;

public interface ValidarEmitirFactura {
    void validate(EmitirFacturaDTO data);
}