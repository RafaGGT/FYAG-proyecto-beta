package com.inacap.fyagobras.domain.user.dto;

public record CredencialesTemporalesDTO(
        DetallesUsuarioDTO usuario,
        String passwordTemporal) {}