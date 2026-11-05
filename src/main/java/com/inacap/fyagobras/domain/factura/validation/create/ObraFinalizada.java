package com.inacap.fyagobras.domain.factura.validation.create;

import com.inacap.fyagobras.domain.factura.dto.EmitirFacturaDTO;
import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.obra.repository.ObraRepository;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ObraFinalizada implements ValidarEmitirFactura {

    @Autowired
    private ObraRepository repository;

    @Override
    public void validate(EmitirFacturaDTO data) {
        var obra = repository.findById(data.obraId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
        if (obra.getEstado() != EstadoObra.FINALIZADA) {
            throw new ConflictoNegocioException("Solo se puede facturar una obra en estado FINALIZADA.");
        }
    }
}