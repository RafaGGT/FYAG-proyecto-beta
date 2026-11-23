package com.inacap.fyagobras.domain.user.dto;

// DTO CredencialesTemporalesDTO: transporta datos entre el dominio y la API.
public record CredencialesTemporalesDTO(
        DetallesUsuarioDTO usuario,
        String passwordTemporal) {}
