package com.inacap.fyagobras.domain.servicio.dto;

import com.inacap.fyagobras.domain.servicio.TipoServicio;

public record DetallesTipoServicioDTO(Long id, String nombre, boolean activo) {

    public DetallesTipoServicioDTO(TipoServicio t) {
        this(t.getId(), t.getNombre(), t.isActivo());
    }
}