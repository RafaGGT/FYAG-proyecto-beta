package com.inacap.fyagobras.domain.cliente.dto;

import com.inacap.fyagobras.domain.cliente.Cliente;

public record DetallesClienteDTO(
        Long id,
        String rut,
        String razonSocial,
        String direccion,
        String email,
        String telefono
) {
    public DetallesClienteDTO(Cliente c) {
        this(c.getId(),
                c.getRut(),
                c.getRazonSocial(),
                c.getDireccion(),
                c.getEmail(),
                c.getTelefono());
    }
}