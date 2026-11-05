package com.inacap.fyagobras.domain.factura.validation.pago;

import com.inacap.fyagobras.domain.factura.Factura;
import com.inacap.fyagobras.domain.factura.dto.RegistrarPagoDTO;

public interface ValidarRegistrarPago {
    void validate(Factura factura, RegistrarPagoDTO data);
}