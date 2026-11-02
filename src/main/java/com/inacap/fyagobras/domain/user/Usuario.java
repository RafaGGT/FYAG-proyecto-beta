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

// Entidad de la tabla usuarios. Implementa UserDetails para que Spring Security la use al autenticar.
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

    // Perfil de acceso (RBAC): un solo rol por usuario
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    // Las cuentas no se eliminan, se desactivan
    @Column(nullable = false)
    private boolean activo = true;

    // Indica si el usuario debe cambiar su contraseña en el próximo login
    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword = false;

    // Control de fuerza bruta: intentos seguidos fallidos y fecha hasta la que la cuenta queda bloqueada
    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    // El rol se expone como ROLE_X para que funcione @PreAuthorize("hasRole('X')")
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }
    // La cuenta está bloqueada mientras bloqueado_hasta sea una fecha futura
    @Override
    public boolean isAccountNonLocked() {
        return bloqueadoHasta == null || bloqueadoHasta.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }
    // Una cuenta desactivada no puede iniciar sesión
    @Override
    public boolean isEnabled() {
        return this.activo;
    }
    // Crea el usuario con la contraseña ya codificada (nunca en texto plano)
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
    // "jUAN" -> "Juan"
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
    // Solo actualiza los campos que vienen informados
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
    // Reset de clave por el Admin: obliga a cambiarla y desbloquea la cuenta
    public void asignarPasswordTemporal(String passwordCodificada) {
        this.password = passwordCodificada;
        this.debeCambiarPassword = true;
        desbloquear();
    }
    // Suma un fallo; al llegar al máximo bloquea la cuenta por un tiempo y reinicia el contador
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