package com.inacap.fyagobras.domain.obra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegistrarObraDTO(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 30, message = "El nombre no puede superar 30 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        String descripcion,

        @NotNull(message = "Debe seleccionar un presupuesto aprobado")
        Long presupuestoId,

        @NotNull(message = "Debe seleccionar un tipo de servicio")
        Long tipoServicioId,

        @NotNull(message = "Debe asignar un técnico")
        Long tecnicoId,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio
) {
}
