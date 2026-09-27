package com.inacap.fyagobras.domain.user.dto;

import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;

public record DetallesUsuarioDTO(
        Long id, String nombre, String apellido, String rut, String email,
        String telefono, String username, Role role, boolean activo,
        boolean debeCambiarPassword, boolean bloqueado
) {
    public DetallesUsuarioDTO(Usuario usuario) {
        this(usuario.getId(), usuario.getNombre(), usuario.getApellido(), usuario.getRut(),
                usuario.getEmail(), usuario.getTelefono(), usuario.getUsername(), usuario.getRole(),
                usuario.isEnabled(), usuario.isDebeCambiarPassword(), !usuario.isAccountNonLocked());
    }
}