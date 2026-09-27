package com.inacap.fyagobras.domain.user.dto;

import com.inacap.fyagobras.domain.user.Role;
import jakarta.validation.constraints.NotNull;

public record ModificarRolDTO(
        @NotNull(message = "El rol es obligatorio")
        Role role
) {
}