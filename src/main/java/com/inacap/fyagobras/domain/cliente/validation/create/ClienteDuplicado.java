package com.inacap.fyagobras.domain.cliente.validation.create;

import com.inacap.fyagobras.domain.cliente.dto.RegistrarClienteDTO;
import com.inacap.fyagobras.domain.cliente.repository.ClienteRepository;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClienteDuplicado implements ValidarCrearCliente {

    @Autowired
    private ClienteRepository repository;

    @Override
    public void validate(RegistrarClienteDTO data) {
        if (repository.findByRut(data.rut().toUpperCase()).isPresent()) {
            throw new ConflictoNegocioException("Ya existe un cliente con este RUT.");
        }
    }
}