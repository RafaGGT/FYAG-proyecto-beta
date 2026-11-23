package com.inacap.fyagobras.domain.factura.validation.create;

import com.inacap.fyagobras.domain.factura.dto.EmitirFacturaDTO;
import com.inacap.fyagobras.domain.factura.repository.FacturaRepository;
import com.inacap.fyagobras.domain.obra.repository.ObraRepository;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
// Validador MontoNoExcedeSaldo: aplica una regla del dominio.
public class MontoNoExcedeSaldo implements ValidarEmitirFactura {

    @Autowired
    private ObraRepository obraRepository;
    @Autowired
    private FacturaRepository facturaRepository;

    @Override
    // Ejecuta la operacion validate.
    public void validate(EmitirFacturaDTO data) {
        var obra = obraRepository.findById(data.obraId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
        var saldo = obra.getPresupuesto().getMontoTotal()
                .subtract(facturaRepository.totalFacturadoObra(obra.getId()));
        if (data.monto().compareTo(saldo) > 0) {
            throw new ConflictoNegocioException(
                    "El monto supera el saldo por facturar de la obra ($" + saldo.toPlainString() + ").");
        }
    }
}
