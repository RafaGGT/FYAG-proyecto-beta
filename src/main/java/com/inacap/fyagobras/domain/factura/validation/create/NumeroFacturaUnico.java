package com.inacap.fyagobras.domain.factura.validation.create;

import com.inacap.fyagobras.domain.factura.dto.EmitirFacturaDTO;
import com.inacap.fyagobras.domain.factura.repository.FacturaRepository;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NumeroFacturaUnico implements ValidarEmitirFactura {

    @Autowired
    private FacturaRepository repository;

    @Override
    public void validate(EmitirFacturaDTO data) {
        if (repository.existsByNumeroFacturaIgnoreCase(data.numeroFactura().trim())) {
            throw new ConflictoNegocioException("Ya existe una factura con ese número.");
        }
    }
}