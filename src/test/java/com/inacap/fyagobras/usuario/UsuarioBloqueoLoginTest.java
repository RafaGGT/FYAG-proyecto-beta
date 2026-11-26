package com.inacap.fyagobras.user;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import com.inacap.fyagobras.domain.user.UsuarioService;
import com.inacap.fyagobras.domain.user.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * CP: Bloqueo de la cuenta tras intentos fallidos, desbloqueo al ingresar bien, cuenta desactivada
 * y registro de todos los intentos de ingreso.
 * Trazabilidad: HU-23, HU-26, RNF-01, OWASP A07. Tipo: seguridad (unitaria). Herramienta: JUnit 5 (automatizada).
 *
 * Los límites se fijan con ReflectionTestUtils a los valores por defecto de application.properties:
 * 5 intentos fallidos y 15 minutos de bloqueo (LOGIN_MAX_INTENTOS y LOGIN_BLOQUEO_MINUTOS).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Bloqueo de cuenta y registro de intentos de ingreso (HU-23 / HU-26 / RNF-01)")
// Prueba la protección contra fuerza bruta que aplica el servicio de usuarios.
class UsuarioBloqueoLoginTest {

    private static final Long USUARIO_ID = 5L;
    private static final String USERNAME = "tecnico1";

    // Simula el repositorio de usuarios y la auditoría.
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaService auditoriaService;
    // Servicio real que se prueba con los mocks anteriores.
    @InjectMocks private UsuarioService service;

    private Usuario tecnico;

    @BeforeEach
    void preparar() {
        // Mockito no resuelve @Value, por eso los límites se fijan manualmente.
        ReflectionTestUtils.setField(service, "maxIntentos", 5);
        ReflectionTestUtils.setField(service, "bloqueoMinutos", 15L);

        // Crea un técnico activo y sin intentos fallidos.
        tecnico = new Usuario();
        tecnico.setId(USUARIO_ID);
        tecnico.setUsername(USERNAME);
        tecnico.setRole(Role.TECNICO);
        tecnico.setActivo(true);
    }

    @Test
    @DisplayName("Con 4 intentos fallidos la cuenta todavía no se bloquea")
    void cuatroFallosNoBloquean() {
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(tecnico));

        // Simula 4 contraseñas incorrectas seguidas.
        for (int i = 0; i < 4; i++) {
            service.registrarFalloLogin(USERNAME);
        }

        // El contador sube, pero la cuenta sigue habilitada para ingresar.
        assertThat(tecnico.getIntentosFallidos()).isEqualTo(4);
        assertThat(tecnico.getBloqueadoHasta()).isNull();
        assertThat(tecnico.isAccountNonLocked()).isTrue();
        // Cada intento fallido queda registrado en la auditoría.
        verify(auditoriaService, times(4)).registrar(eq(tecnico), eq("usuarios"), eq(USUARIO_ID),
                eq("LOGIN_FALLIDO"), isNull(), isNull(), anyMap());
    }

    @Test
    @DisplayName("Al 5.º intento fallido la cuenta se bloquea por 15 minutos y el contador se reinicia")
    void quintoFalloBloqueaPor15Minutos() {
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(tecnico));
        var antes = LocalDateTime.now();

        // Simula 5 contraseñas incorrectas seguidas.
        for (int i = 0; i < 5; i++) {
            service.registrarFalloLogin(USERNAME);
        }

        // La cuenta queda bloqueada hasta unos 15 minutos después.
        assertThat(tecnico.isAccountNonLocked()).isFalse();
        assertThat(tecnico.getBloqueadoHasta())
                .isAfter(antes.plusMinutes(14))
                .isBefore(LocalDateTime.now().plusMinutes(16));
        assertThat(tecnico.getIntentosFallidos()).isZero();
        // El último intento deja constancia de que la cuenta quedó bloqueada.
        verify(auditoriaService).registrar(eq(tecnico), eq("usuarios"), eq(USUARIO_ID), eq("LOGIN_FALLIDO"),
                isNull(), isNull(),
                argThat((Map<String, Object> m) -> Boolean.TRUE.equals(m.get("cuenta_bloqueada"))));
    }

    @Test
    @DisplayName("Cuando vence el tiempo de bloqueo la cuenta vuelve a poder ingresar")
    void bloqueoVencidoPermiteIngresar() {
        // El bloqueo terminó hace un minuto.
        tecnico.setBloqueadoHasta(LocalDateTime.now().minusMinutes(1));

        assertThat(tecnico.isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("Un ingreso exitoso reinicia el contador de intentos fallidos y queda auditado")
    void ingresoExitosoReiniciaContador() {
        // La cuenta traía 3 intentos fallidos acumulados.
        tecnico.setIntentosFallidos(3);
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(tecnico));

        service.registrarExitoLogin(USERNAME);

        assertThat(tecnico.getIntentosFallidos()).isZero();
        assertThat(tecnico.getBloqueadoHasta()).isNull();
        verify(auditoriaService).registrar(eq(tecnico), eq("usuarios"), eq(USUARIO_ID), eq("LOGIN_EXITOSO"),
                isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("Un fallo con un usuario que no existe no genera auditoría ni error")
    void usuarioInexistenteNoGeneraAuditoria() {
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        service.registrarFalloLogin("fantasma");

        verifyNoInteractions(auditoriaService);
    }

    @ParameterizedTest(name = "Intento contra cuenta rechazada: {0}")
    @ValueSource(strings = {"LOGIN_CUENTA_BLOQUEADA", "LOGIN_CUENTA_DESACTIVADA"})
    @DisplayName("Los intentos de ingreso a una cuenta bloqueada o desactivada también quedan registrados")
    void intentosRechazadosSeAuditan(String accion) {
        when(usuarioRepository.findByUsername(USERNAME)).thenReturn(Optional.of(tecnico));

        service.registrarIntentoRechazado(USERNAME, accion);

        verify(auditoriaService).registrar(eq(tecnico), eq("usuarios"), eq(USUARIO_ID), eq(accion),
                isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("Una cuenta desactivada no puede iniciar sesión (isEnabled es falso)")
    void cuentaDesactivadaNoPuedeIngresar() {
        tecnico.setActivo(false);

        // Spring Security rechaza el ingreso cuando isEnabled devuelve falso.
        assertThat(tecnico.isEnabled()).isFalse();
    }

    @ParameterizedTest(name = "Rol {0} se expone como ROLE_{0}")
    @EnumSource(Role.class)
    @DisplayName("Cada uno de los cinco perfiles se traduce a la autoridad que usa @PreAuthorize")
    void cadaRolSeExponeComoAutoridad(Role rol) {
        tecnico.setRole(rol);

        assertThat(tecnico.getAuthorities()).extracting("authority").containsExactly("ROLE_" + rol.name());
    }
}