package com.inacap.fyagobras.domain.cliente.validation.create;

import com.inacap.fyagobras.domain.cliente.dto.RegistrarClienteDTO;

// Validador ValidarCrearCliente: aplica una regla del dominio.
public interface ValidarCrearCliente {
    // Ejecuta la operacion validate.
    void validate(RegistrarClienteDTO data);
}
