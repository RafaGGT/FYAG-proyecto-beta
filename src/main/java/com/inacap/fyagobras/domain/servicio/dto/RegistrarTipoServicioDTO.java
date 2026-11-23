package com.inacap.fyagobras.domain.servicio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// DTO RegistrarTipoServicioDTO: transporta datos entre el dominio y la API.
public record RegistrarTipoServicioDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre
) {
}
