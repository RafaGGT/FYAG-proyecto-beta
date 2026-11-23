package com.inacap.fyagobras.domain.user.dto;

import com.inacap.fyagobras.domain.user.Role;
import jakarta.validation.constraints.NotNull;

// DTO ModificarRolDTO: transporta datos entre el dominio y la API.
public record ModificarRolDTO(
        @NotNull(message = "El rol es obligatorio")
        Role role
) {
}
