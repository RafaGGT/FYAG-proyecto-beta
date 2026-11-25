package com.inacap.fyagobras.user;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import com.inacap.fyagobras.domain.user.UsuarioService;
import com.inacap.fyagobras.domain.user.dto.CambiarPasswordDTO;
import com.inacap.fyagobras.domain.user.dto.ModificarRolDTO;
import com.inacap.fyagobras.domain.user.dto.RegistrarUsuarioDTO;
import com.inacap.fyagobras.domain.user.dto.UsuarioActivoDTO;
import com.inacap.fyagobras.domain.user.repository.UsuarioRepository;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CP: Alta de cuentas con contraseña temporal, cambio de perfil, desactivación sin borrar, perfil ADMIN
 * protegido y cambio de contraseña.
 * Trazabilidad: HU-21, HU-22, HU-26, RNF-02, OWASP A02 y A07. Tipo: seguridad (unitaria).
 * Herramienta: JUnit 5 y Mockito (automatizada).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Gestión de cuentas y contraseñas (HU-21 / HU-22 / HU-26)")
// Prueba que las cuentas se administren sin exponer ni perder información de seguridad.
class UsuarioCuentaTest {

    // Simula el repositorio, el codificador de contraseñas y la auditoría.
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditoriaService auditoriaService;
    // Servicio real que se prueba con los mocks anteriores.
    @InjectMocks private UsuarioService service;

    private Usuario admin;
    private Usuario tecnico;

    @BeforeEach
    void preparar() {
        // Sin validadores de duplicados: aquí solo interesa lo que hace el servicio al registrar.
        ReflectionTestUtils.setField(service, "crearValidador", List.of());

        // Administrador que ejecuta las acciones.
        admin = usuario(1L, "admin", Role.ADMIN);
        // Técnico sobre el que se actúa.
        tecnico = usuario(7L, "jperez", Role.TECNICO);
        tecnico.setPassword("hashActual");
    }

    private static Usuario usuario(Long id, String username, Role rol) {
        // Construye un usuario activo con el rol indicado.
        var u = new Usuario();
        u.setId(id);
        u.setUsername(username);
        u.setRole(rol);
        u.setActivo(true);
        return u;
    }

    // ---------- HU-21: alta con contraseña temporal ----------

    @Test
    @DisplayName("Al crear una cuenta la contraseña se guarda con hash, debe cambiarse y la auditoría no la incluye")
    void altaGuardaHashObligaCambioYNoAuditaLaClave() {
        var datos = new RegistrarUsuarioDTO("Juan", "Pérez", "jperez@fyag.cl", "+56912345678",
                "12345678-5", "jperez", "Temporal#2026", Role.TECNICO);
        when(passwordEncoder.encode("Temporal#2026")).thenReturn("$2a$10$hashDePrueba");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(7L);
            return u;
        });

        var resultado = service.registrar(datos, admin);

        // La entidad guardada tiene el hash y nunca la contraseña original.
        var captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$10$hashDePrueba").isNotEqualTo("Temporal#2026");
        // El usuario queda obligado a cambiar la contraseña en su primer ingreso.
        assertThat(captor.getValue().isDebeCambiarPassword()).isTrue();
        assertThat(resultado.debeCambiarPassword()).isTrue();
        assertThat(resultado.role()).isEqualTo(Role.TECNICO);
        // La auditoría registra quién creó la cuenta y con qué perfil, sin ninguna contraseña.
        verify(auditoriaService).registrar(eq(admin), eq("usuarios"), eq(7L), eq("CREAR_USUARIO"),
                isNull(), isNull(),
                argThat((Map<String, Object> m) -> "jperez".equals(m.get("username"))
                        && "TECNICO".equals(m.get("role")) && !m.containsKey("password")));
    }

    // ---------- HU-22: cambio de perfil ----------

    @Test
    @DisplayName("El Administrador cambia el perfil de un usuario y queda auditado el perfil anterior y el nuevo")
    void cambioDePerfilQuedaAuditado() {
        when(usuarioRepository.findByUsername("jperez")).thenReturn(Optional.of(tecnico));

        var resultado = service.administrarCuenta("jperez", new ModificarRolDTO(Role.SUPERVISOR), admin);

        assertThat(resultado.getRole()).isEqualTo(Role.SUPERVISOR);
        verify(auditoriaService).registrar(eq(admin), eq("usuarios"), eq(7L), eq("CAMBIAR_ROL"), isNull(),
                eq(Map.<String, Object>of("role", "TECNICO")),
                eq(Map.<String, Object>of("role", "SUPERVISOR")));
    }

    @Test
    @DisplayName("El perfil ADMIN es intocable: no se desactiva, no se resetea y no se asigna a otro usuario")
    void perfilAdminEsIntocable() {
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByUsername("jperez")).thenReturn(Optional.of(tecnico));

        // No se puede desactivar la cuenta ADMIN.
        assertThatThrownBy(() -> service.cambiarEstado("admin", new UsuarioActivoDTO(false), admin))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("ADMIN es intocable");
        // No se puede resetear la contraseña del ADMIN.
        assertThatThrownBy(() -> service.resetearPassword("admin", admin))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("ADMIN es intocable");
        // No se puede ascender a otro usuario a ADMIN (escalada de privilegios).
        assertThatThrownBy(() -> service.administrarCuenta("jperez", new ModificarRolDTO(Role.ADMIN), admin))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("ADMIN es intocable");

        // Nada cambió y nada se auditó.
        assertThat(admin.isEnabled()).isTrue();
        assertThat(tecnico.getRole()).isEqualTo(Role.TECNICO);
        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    // ---------- HU-26: desactivar sin borrar ----------

    @Test
    @DisplayName("Desactivar una cuenta no la elimina: queda inactiva, conserva su historial y se audita")
    void desactivarCuentaNoElimina() {
        when(usuarioRepository.findByUsername("jperez")).thenReturn(Optional.of(tecnico));

        var resultado = service.cambiarEstado("jperez", new UsuarioActivoDTO(false), admin);

        // La cuenta sigue existiendo, pero no puede ingresar.
        assertThat(resultado.isEnabled()).isFalse();
        verify(usuarioRepository, never()).delete(any(Usuario.class));
        verify(usuarioRepository, never()).deleteById(any());
        // Se registra quién la desactivó y el estado anterior y nuevo.
        verify(auditoriaService).registrar(eq(admin), eq("usuarios"), eq(7L), eq("DESACTIVAR_USUARIO"), isNull(),
                eq(Map.<String, Object>of("activo", true)),
                eq(Map.<String, Object>of("activo", false)));
    }

    // ---------- Contraseñas ----------

    @Test
    @DisplayName("Cambiar contraseña con la actual incorrecta se rechaza y no modifica la clave")
    void cambiarPasswordConActualIncorrecta() {
        when(usuarioRepository.findByUsername("jperez")).thenReturn(Optional.of(tecnico));
        when(passwordEncoder.matches("Mala#2026", "hashActual")).thenReturn(false);

        assertThatThrownBy(() -> service.cambiarPassword("jperez", new CambiarPasswordDTO("Mala#2026", "Nueva#2026")))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("La contraseña actual es incorrecta.");

        assertThat(tecnico.getPassword()).isEqualTo("hashActual");
        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("La nueva contraseña no puede ser igual a la actual")
    void nuevaPasswordDebeSerDistinta() {
        when(usuarioRepository.findByUsername("jperez")).thenReturn(Optional.of(tecnico));
        when(passwordEncoder.matches("Actual#2026", "hashActual")).thenReturn(true);

        assertThatThrownBy(() -> service.cambiarPassword("jperez", new CambiarPasswordDTO("Actual#2026", "Actual#2026")))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("La nueva contraseña debe ser distinta a la actual.");

        assertThat(tecnico.getPassword()).isEqualTo("hashActual");
    }

    @Test
    @DisplayName("Un cambio de contraseña válido guarda el nuevo hash, quita la obligación de cambiarla y se audita")
    void cambiarPasswordValidoQuitaLaObligacion() {
        // El usuario ingresó con una contraseña temporal y debe cambiarla.
        tecnico.setDebeCambiarPassword(true);
        when(usuarioRepository.findByUsername("jperez")).thenReturn(Optional.of(tecnico));
        when(passwordEncoder.matches("Actual#2026", "hashActual")).thenReturn(true);
        when(passwordEncoder.matches("Nueva#2026", "hashActual")).thenReturn(false);
        when(passwordEncoder.encode("Nueva#2026")).thenReturn("hashNueva");

        service.cambiarPassword("jperez", new CambiarPasswordDTO("Actual#2026", "Nueva#2026"));

        assertThat(tecnico.getPassword()).isEqualTo("hashNueva");
        assertThat(tecnico.isDebeCambiarPassword()).isFalse();
        // La auditoría deja constancia del cambio sin guardar ningún valor de la clave.
        verify(auditoriaService).registrar(eq(tecnico), eq("usuarios"), eq(7L), eq("CAMBIAR_PASSWORD"),
                isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("Resetear la contraseña genera una temporal de 12 caracteres, desbloquea la cuenta y no audita la clave")
    void resetearPasswordGeneraTemporalYDesbloquea() {
        // La cuenta estaba bloqueada por intentos fallidos.
        tecnico.setBloqueadoHasta(LocalDateTime.now().plusMinutes(10));
        when(usuarioRepository.findByUsername("jperez")).thenReturn(Optional.of(tecnico));
        when(passwordEncoder.encode(anyString())).thenReturn("hashTemporal");

        var resultado = service.resetearPassword("jperez", admin);

        // La temporal se entrega una sola vez y en la base solo queda su hash.
        assertThat(resultado.passwordTemporal()).hasSize(12);
        assertThat(tecnico.getPassword()).isEqualTo("hashTemporal");
        assertThat(tecnico.isDebeCambiarPassword()).isTrue();
        assertThat(tecnico.isAccountNonLocked()).isTrue();
        // La auditoría solo indica que se exigió el cambio: no contiene la contraseña.
        verify(auditoriaService).registrar(eq(admin), eq("usuarios"), eq(7L), eq("RESETEAR_PASSWORD"),
                isNull(), isNull(),
                argThat((Map<String, Object> m) -> m.size() == 1
                        && Boolean.TRUE.equals(m.get("debe_cambiar_password"))));
    }
}