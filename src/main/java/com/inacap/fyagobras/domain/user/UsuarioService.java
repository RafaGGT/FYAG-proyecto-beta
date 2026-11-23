package com.inacap.fyagobras.domain.user;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.user.dto.*;
import com.inacap.fyagobras.domain.user.repository.UsuarioRepository;
import com.inacap.fyagobras.domain.user.validation.create.ValidarCrearUsuario;
import com.inacap.fyagobras.domain.user.validation.update.ValidarActualizarUsuario;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
// Clase UsuarioService: implementa la funcionalidad del dominio.
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private List<ValidarCrearUsuario> crearValidador;
    @Autowired
    private List<ValidarActualizarUsuario> actualizarValidador;
    @Autowired
    private AuditoriaService auditoriaService;

    @Value("${app.login.max-intentos:5}")
    private int maxIntentos;
    @Value("${app.login.bloqueo-minutos:15}")
    private long bloqueoMinutos;

    @Transactional
    // Ejecuta la operacion registrar.
    public DetallesUsuarioDTO registrar(RegistrarUsuarioDTO datos, Usuario actor) {
        // Ejecuta las reglas de validación para el nuevo usuario.
        crearValidador.forEach(v -> v.validate(datos));
        // Hashea la contraseña antes de construir la entidad.
        var usuario = new Usuario(datos, passwordEncoder.encode(datos.password()));
        // Obliga al usuario a definir una contraseña propia en el primer acceso.
        usuario.setDebeCambiarPassword(true); // obliga a cambiarla en su primer login
        // Persiste el usuario y devuelve sus datos públicos.
        var guardado = usuarioRepository.save(usuario);
        // Auditoría: quién creó la cuenta y con qué perfil (nunca se guarda la contraseña).
        auditoriaService.registrar(actor, "usuarios", guardado.getId(), "CREAR_USUARIO", null, null,
                Map.of("username", guardado.getUsername(), "role", guardado.getRole().name()));
        return new DetallesUsuarioDTO(guardado);
    }

    // Ejecuta la operacion obtenerPorUsername.
    public Usuario obtenerPorUsername(String username) {
        // Ejecuta la operacion buscar.
        return buscar(username);
    }

    // Ejecuta la operacion listar.
    public Page<Usuario> listar(Role role, Boolean activo, Pageable paginacion) {
        // Elige la consulta específica según los filtros recibidos.
        if (role != null && activo != null) {
            return usuarioRepository.findByRoleAndActivo(role, activo, paginacion);
        }
        if (role != null) {
            return usuarioRepository.findByRole(role, paginacion);
        }
        if (activo != null) {
            return usuarioRepository.findByActivo(activo, paginacion);
        }
        return usuarioRepository.findAll(paginacion);
    }

    // Ejecuta la operacion listarTecnicos.
    public List<Usuario> listarTecnicos() {
        // Devuelve solo técnicos activos ordenados por apellido.
        return usuarioRepository.findByRoleAndActivoTrueOrderByApellidoAsc(Role.TECNICO);
    }

    @Transactional
    // Ejecuta la operacion administrarCuenta.
    public Usuario administrarCuenta(String username, ModificarRolDTO datos, Usuario actor) {
        // Busca la cuenta que se desea administrar.
        var usuario = buscar(username);

        // Protege las cuentas ADMIN de cambios desde este flujo.
        if (usuario.getRole() == Role.ADMIN || datos.role() == Role.ADMIN) {
            throw new ConflictoNegocioException(
                    "El rol ADMIN es intocable: no se puede asignar ni modificar desde este endpoint.");
        }

        // Se guarda el rol anterior ANTES de aplicar el cambio.
        var rolAnterior = usuario.getRole().name();

        // Aplica el nuevo rol u otros datos administrativos.
        usuario.actualizarCuenta(datos);

        // Auditoría: cambio de perfil (RBAC) con valor anterior y nuevo.
        auditoriaService.registrar(actor, "usuarios", usuario.getId(), "CAMBIAR_ROL", null,
                Map.of("role", rolAnterior), Map.of("role", usuario.getRole().name()));
        return usuario;
    }

    @Transactional
    // Ejecuta la operacion actualizarDatos.
    public Usuario actualizarDatos(String username, ModificarUsuarioDTO datos) {
        // Valida los datos antes de modificar la cuenta.
        actualizarValidador.forEach(v -> v.validate(username, datos));
        // Recupera la entidad administrada por JPA.
        var usuario = buscar(username);

        // Actualiza los datos personales permitidos.
        usuario.actualizarUsuario(datos);
        return usuario;
    }


    @Transactional
    // Ejecuta la operacion cambiarEstado.
    public Usuario cambiarEstado(String username, UsuarioActivoDTO datos, Usuario actor) {
        // Obtiene el usuario que se quiere activar o desactivar.
        var usuario = buscar(username);

        // La cuenta ADMIN no puede ser deshabilitada por este endpoint.
        if (usuario.getRole() == Role.ADMIN) {
            throw new ConflictoNegocioException(
                    "El rol ADMIN es intocable: no se puede activar/desactivar desde este endpoint.");
        }

        // Se guarda el estado anterior ANTES de aplicar el cambio.
        var estabaActivo = usuario.isEnabled();
        usuario.cambiarEstado(datos);

        // Auditoría: activación o desactivación de la cuenta (HU-26).
        auditoriaService.registrar(actor, "usuarios", usuario.getId(),
                datos.activo() ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO", null,
                Map.of("activo", estabaActivo), Map.of("activo", datos.activo()));
        return usuario;
    }

    // --- contraseña y bloqueo ---

    @Transactional
    // Ejecuta la operacion cambiarPassword.
    public void cambiarPassword(String username, CambiarPasswordDTO datos) {
        // Busca la cuenta que solicita el cambio.
        var usuario = buscar(username);

        // Comprueba la contraseña actual antes de aceptar la nueva.
        if (!passwordEncoder.matches(datos.passwordActual(), usuario.getPassword())) {
            throw new ConflictoNegocioException("La contraseña actual es incorrecta.");
        }
        // Evita reutilizar la contraseña vigente.
        if (passwordEncoder.matches(datos.passwordNueva(), usuario.getPassword())) {
            throw new ConflictoNegocioException("La nueva contraseña debe ser distinta a la actual.");
        }

        // Guarda la nueva contraseña hasheada.
        usuario.cambiarPassword(passwordEncoder.encode(datos.passwordNueva()));

        // Auditoría: el propio usuario cambió su clave (sin guardar ningún valor de la clave).
        auditoriaService.registrar(usuario, "usuarios", usuario.getId(), "CAMBIAR_PASSWORD", null, null, null);
    }

    @Transactional
    // Ejecuta la operacion resetearPassword.
    public CredencialesTemporalesDTO resetearPassword(String username, Usuario actor) {
        // Recupera la cuenta a la que se asignará una contraseña temporal.
        var usuario = buscar(username);

        // La contraseña de ADMIN no se puede resetear desde este flujo.
        if (usuario.getRole() == Role.ADMIN) {
            throw new ConflictoNegocioException(
                    "El rol ADMIN es intocable: no se puede resetear su contraseña desde este endpoint.");
        }

        // Genera, hashea y asigna una contraseña temporal.
        var temporal = GeneradorPassword.generar(12);
        usuario.asignarPasswordTemporal(passwordEncoder.encode(temporal));

        // Auditoría: se registra el reset, JAMÁS la contraseña temporal.
        auditoriaService.registrar(actor, "usuarios", usuario.getId(), "RESETEAR_PASSWORD", null, null,
                Map.of("debe_cambiar_password", true));
        return new CredencialesTemporalesDTO(new DetallesUsuarioDTO(usuario), temporal);
    }

    @Transactional
    // Ejecuta la operacion desbloquear.
    public Usuario desbloquear(String username, Usuario actor) {
        // Busca la cuenta y elimina su bloqueo de acceso.
        var usuario = buscar(username);
        usuario.desbloquear();

        // Auditoría: quién levantó el bloqueo por intentos fallidos.
        auditoriaService.registrar(actor, "usuarios", usuario.getId(), "DESBLOQUEAR_USUARIO", null, null, null);
        return usuario;
    }

    // --- auditoría de intentos de ingreso ---

    @Transactional
    // Ejecuta la operacion registrarFalloLogin.
    public void registrarFalloLogin(String username) {
        // Si la cuenta existe, acumula el intento fallido y aplica el bloqueo configurado.
        usuarioRepository.findByUsername(username).ifPresent(u -> {
            u.registrarFallo(maxIntentos, Duration.ofMinutes(bloqueoMinutos));
            // La cuenta está bloqueada solo si la fecha de bloqueo es futura.
            boolean bloqueada = u.getBloqueadoHasta() != null
                    && u.getBloqueadoHasta().isAfter(LocalDateTime.now());
            // Auditoría: cada intento fallido queda registrado.
            auditoriaService.registrar(u, "usuarios", u.getId(), "LOGIN_FALLIDO", null, null,
                    Map.of("intentos_fallidos", u.getIntentosFallidos(), "cuenta_bloqueada", bloqueada));
        });
    }

    @Transactional
    // Ejecuta la operacion registrarExitoLogin.
    public void registrarExitoLogin(String username) {
        // Un inicio de sesión exitoso limpia los contadores de bloqueo.
        usuarioRepository.findByUsername(username).ifPresent(u -> {
            if (u.getIntentosFallidos() > 0 || u.getBloqueadoHasta() != null) {
                u.desbloquear();
            }
            // Auditoría: cada ingreso exitoso queda registrado.
            auditoriaService.registrar(u, "usuarios", u.getId(), "LOGIN_EXITOSO", null, null, null);
        });
    }

    @Transactional
    // Ejecuta la operacion registrarIntentoRechazado.
    public void registrarIntentoRechazado(String username, String accion) {
        // Intentos rechazados porque la cuenta está bloqueada o desactivada.
        usuarioRepository.findByUsername(username).ifPresent(u ->
                auditoriaService.registrar(u, "usuarios", u.getId(), accion, null, null, null));
    }

    // Ejecuta la operacion buscar.
    private Usuario buscar(String username) {
        // Centraliza la consulta de usuarios y el error cuando no existe.
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + username));
    }
}
