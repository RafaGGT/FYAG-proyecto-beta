package com.inacap.fyagobras.domain.factura.validation.pago;

import com.inacap.fyagobras.domain.factura.EstadoPago;
import com.inacap.fyagobras.domain.factura.Factura;
import com.inacap.fyagobras.domain.factura.dto.RegistrarPagoDTO;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import org.springframework.stereotype.Component;

@Component
// Validador FacturaPendiente: aplica una regla del dominio.
public class FacturaPendiente implements ValidarRegistrarPago {

    @Override
    // Ejecuta la operacion validate.
    public void validate(Factura factura, RegistrarPagoDTO data) {
        if (factura.isAnulada()) {
            throw new ConflictoNegocioException("La factura está anulada y no admite pagos.");
        }
        if (factura.getEstadoPago() == EstadoPago.PAGADO) {
            throw new ConflictoNegocioException("La factura ya se encuentra pagada.");
        }
    }
}
