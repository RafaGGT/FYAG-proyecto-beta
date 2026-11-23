package com.inacap.fyagobras.domain.user.dto;

// DTO TecnicoDTO: transporta datos entre el dominio y la API.
public record TecnicoDTO(
        Long id,
        String nombre,
        String apellido) {}
