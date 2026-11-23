package com.inacap.fyagobras.domain.factura.validation.pago;

import com.inacap.fyagobras.domain.factura.Factura;
import com.inacap.fyagobras.domain.factura.dto.RegistrarPagoDTO;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import org.springframework.stereotype.Component;

@Component
// Validador FechaPagoValida: aplica una regla del dominio.
public class FechaPagoValida implements ValidarRegistrarPago {

    @Override
    // Ejecuta la operacion validate.
    public void validate(Factura factura, RegistrarPagoDTO data) {
        if (data.fechaPago().isBefore(factura.getFechaEmision())) {
            throw new ConflictoNegocioException(
                    "La fecha de pago no puede ser anterior a la fecha de emisión de la factura.");
        }
    }
}
