package com.inacap.fyagobras.domain.cliente.validation.create;

import com.inacap.fyagobras.domain.cliente.dto.RegistrarClienteDTO;

public interface ValidarCrearCliente {
    void validate(RegistrarClienteDTO data);
}