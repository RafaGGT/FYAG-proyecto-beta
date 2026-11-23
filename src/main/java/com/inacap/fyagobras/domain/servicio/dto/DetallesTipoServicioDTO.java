package com.inacap.fyagobras.domain.servicio.dto;

import com.inacap.fyagobras.domain.servicio.TipoServicio;

// DTO DetallesTipoServicioDTO: transporta datos entre el dominio y la API.
public record DetallesTipoServicioDTO(Long id, String nombre, boolean activo) {

    // Ejecuta la operacion DetallesTipoServicioDTO.
    public DetallesTipoServicioDTO(TipoServicio t) {
        this(t.getId(), t.getNombre(), t.isActivo());
    }
}
