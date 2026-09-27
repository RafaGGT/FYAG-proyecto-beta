package com.inacap.fyagobras.domain.user;

import com.inacap.fyagobras.domain.user.dto.ModificarRolDTO;
import com.inacap.fyagobras.domain.user.dto.ModificarUsuarioDTO;
import com.inacap.fyagobras.domain.user.dto.RegistrarUsuarioDTO;
import com.inacap.fyagobras.domain.user.dto.UsuarioActivoDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "password")
public class Usuario extends Persona implements UserDetails {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword = false;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return bloqueadoHasta == null || bloqueadoHasta.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return this.activo;
    }

    public Usuario(RegistrarUsuarioDTO datos, String passwordCodificada) {
        this.nombre = datos.nombre();
        this.apellido = datos.apellido();
        this.email = datos.email();
        this.telefono = datos.telefono();
        this.rut = datos.rut();
        this.username = datos.username();
        this.password = passwordCodificada;
        this.role = datos.role();
    }

    private String capitalizado(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }

    public void actualizarUsuario(ModificarUsuarioDTO datos) {
        if (datos.nombre() != null && !datos.nombre().isBlank()) {
            this.nombre = capitalizado(datos.nombre());
        }
        if (datos.apellido() != null && !datos.apellido().isBlank()) {
            this.apellido = capitalizado(datos.apellido());
        }
        if (datos.email() != null) {
            this.email = datos.email();
        }
        if (datos.telefono() != null) {
            this.telefono = datos.telefono();
        }
    }

    public void actualizarCuenta(ModificarRolDTO datos) {
        if (datos.role() != null) {
            this.role = datos.role();
        }
    }

    public void cambiarEstado(UsuarioActivoDTO datos) {
        this.activo = datos.activo();
    }

    public void desactivar() {
        this.activo = false;
    }

    // --- contraseña y bloqueo ---

    public void cambiarPassword(String passwordCodificada) {
        this.password = passwordCodificada;
        this.debeCambiarPassword = false;
    }

    public void asignarPasswordTemporal(String passwordCodificada) {
        this.password = passwordCodificada;
        this.debeCambiarPassword = true;
        desbloquear();
    }

    public void registrarFallo(int maxIntentos, Duration duracionBloqueo) {
        this.intentosFallidos++;
        if (this.intentosFallidos >= maxIntentos) {
            this.bloqueadoHasta = LocalDateTime.now().plus(duracionBloqueo);
            this.intentosFallidos = 0;
        }
    }

    public void desbloquear() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }
}