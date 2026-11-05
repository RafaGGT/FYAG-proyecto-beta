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
// Excluye el campo "password" de la representación en cadena (toString) generada por Lombok por motivos de seguridad (evita imprimir contraseñas en los logs)
@ToString(exclude = "password")
public class Usuario extends Persona implements UserDetails {

    // Define la columna del nombre de usuario única y obligatoria
    @Column(nullable = false, unique = true)
    private String username;

    // Define la columna obligatoria para la contraseña del usuario
    @Column(nullable = false)
    private String password;

    // Perfil de acceso (RBAC): un solo rol por usuario, almacenado como texto en la base de datos
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    
    // Las cuentas no se eliminan, se desactivan mediante este indicador booleano
    @Column(nullable = false)
    private boolean activo = true;

    // Indica si el usuario debe cambiar su contraseña en el próximo login
    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword = false;

    // Control de fuerza bruta: intentos seguidos fallidos y fecha hasta la que la cuenta queda bloqueada
    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    // Almacena la fecha y hora límite hasta la cual la cuenta permanece bloqueada
    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    // El rol se expone como ROLE_X para que funcione @PreAuthorize("hasRole('X')")
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Retorna la lista con la autoridad mapeada con el prefijo "ROLE_" requerido por Spring Security
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        // Indica si la cuenta ha expirado (por defecto retorna true delegando en UserDetails)
        return UserDetails.super.isAccountNonExpired();
    }
    
    // La cuenta está bloqueada mientras bloqueado_hasta sea una fecha futura
    @Override
    public boolean isAccountNonLocked() {
        // Retorna true si no hay fecha de bloqueo o si la fecha límite de bloqueo ya pasó
        return bloqueadoHasta == null || bloqueadoHasta.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // Indica si las credenciales han expirado (por defecto retorna true)
        return UserDetails.super.isCredentialsNonExpired();
    }
    
    // Una cuenta desactivada no puede iniciar sesión
    @Override
    public boolean isEnabled() {
        // Retorna el estado activo actual de la entidad
        return this.activo;
    }
    
    // Crea el usuario con la contraseña ya codificada (nunca en texto plano)
    public Usuario(RegistrarUsuarioDTO datos, String passwordCodificada) {
        // Asigna los atributos heredados de la superclase Persona
        this.nombre = datos.nombre();
        this.apellido = datos.apellido();
        this.email = datos.email();
        this.telefono = datos.telefono();
        this.rut = datos.rut();
        // Asigna los atributos propios de Usuario
        this.username = datos.username();
        this.password = passwordCodificada;
        this.role = datos.role();
    }
    
    // Transforma una cadena de texto a formato capitalizado (ej. "jUAN" -> "Juan")
    private String capitalizado(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }

    // Actualiza los datos personales del usuario solo si vienen informados en el DTO
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
    
    // Solo actualiza los campos de la cuenta que vienen informados (como el rol)
    public void actualizarCuenta(ModificarRolDTO datos) {
        if (datos.role() != null) {
            this.role = datos.role();
        }
    }

    // Cambia el estado de activación de la cuenta según los datos proporcionados
    public void cambiarEstado(UsuarioActivoDTO datos) {
        this.activo = datos.activo();
    }

    // Desactiva la cuenta de usuario de forma directa
    public void desactivar() {
        this.activo = false;
    }

    // --- contraseña y bloqueo ---

    // Actualiza la contraseña del usuario y desactiva la obligación de cambiarla
    public void cambiarPassword(String passwordCodificada) {
        this.password = passwordCodificada;
        this.debeCambiarPassword = false;
    }
    
    // Reset de clave por el Admin: obliga a cambiarla en el próximo acceso y desbloquea la cuenta
    public void asignarPasswordTemporal(String passwordCodificada) {
        this.password = passwordCodificada;
        this.debeCambiarPassword = true;
        desbloquear();
    }
    
    // Suma un fallo; al llegar al máximo bloquea la cuenta por un tiempo determinado y reinicia el contador
    public void registrarFallo(int maxIntentos, Duration duracionBloqueo) {
        this.intentosFallidos++;
        if (this.intentosFallidos >= maxIntentos) {
            this.bloqueadoHasta = LocalDateTime.now().plus(duracionBloqueo);
            this.intentosFallidos = 0;
        }
    }

    // Restablece los contadores de fallos y elimina la fecha límite de bloqueo
    public void desbloquear() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }
}