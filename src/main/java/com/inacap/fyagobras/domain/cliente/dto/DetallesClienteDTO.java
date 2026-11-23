package com.inacap.fyagobras.domain.cliente.dto;

import com.inacap.fyagobras.domain.cliente.Cliente;

// DTO DetallesClienteDTO: transporta datos entre el dominio y la API.
public record DetallesClienteDTO(
        Long id,
        String rut,
        String razonSocial,
        String direccion,
        String email,
        String telefono
) {
    // Ejecuta la operacion DetallesClienteDTO.
    public DetallesClienteDTO(Cliente c) {
        this(c.getId(),
                c.getRut(),
                c.getRazonSocial(),
                c.getDireccion(),
                c.getEmail(),
                c.getTelefono());
    }
}
