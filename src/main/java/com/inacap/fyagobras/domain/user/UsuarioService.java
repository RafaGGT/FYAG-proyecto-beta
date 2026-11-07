package com.inacap.fyagobras.domain.user;

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
import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private List<ValidarCrearUsuario> crearValidador;
    @Autowired
    private List<ValidarActualizarUsuario> actualizarValidador;

    @Value("${app.login.max-intentos:5}")
    private int maxIntentos;
    @Value("${app.login.bloqueo-minutos:15}")
    private long bloqueoMinutos;

    @Transactional
    public DetallesUsuarioDTO registrar(RegistrarUsuarioDTO datos) {
        // Ejecuta las reglas de validación para el nuevo usuario.
        crearValidador.forEach(v -> v.validate(datos));
        // Hashea la contraseña antes de construir la entidad.
        var usuario = new Usuario(datos, passwordEncoder.encode(datos.password()));
        // Obliga al usuario a definir una contraseña propia en el primer acceso.
        usuario.setDebeCambiarPassword(true); // obliga a cambiarla en su primer login
        // Persiste el usuario y devuelve sus datos públicos.
        var guardado = usuarioRepository.save(usuario);
        return new DetallesUsuarioDTO(guardado);
    }

    public Usuario obtenerPorUsername(String username) {
        return buscar(username);
    }

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

    public List<Usuario> listarTecnicos() {
        // Devuelve solo técnicos activos ordenados por apellido.
        return usuarioRepository.findByRoleAndActivoTrueOrderByApellidoAsc(Role.TECNICO);
    }

    @Transactional
    public Usuario administrarCuenta(String username, ModificarRolDTO datos) {
        // Busca la cuenta que se desea administrar.
        var usuario = buscar(username);

        // Protege las cuentas ADMIN de cambios desde este flujo.
        if (usuario.getRole() == Role.ADMIN || datos.role() == Role.ADMIN) {
            throw new ConflictoNegocioException(
                    "El rol ADMIN es intocable: no se puede asignar ni modificar desde este endpoint.");
        }

        // Aplica el nuevo rol u otros datos administrativos.
        usuario.actualizarCuenta(datos);
        return usuario;
    }

    @Transactional
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
    public Usuario cambiarEstado(String username, UsuarioActivoDTO datos) {
        // Obtiene el usuario que se quiere activar o desactivar.
        var usuario = buscar(username);

        // La cuenta ADMIN no puede ser deshabilitada por este endpoint.
        if (usuario.getRole() == Role.ADMIN) {
            throw new ConflictoNegocioException(
                    "El rol ADMIN es intocable: no se puede activar/desactivar desde este endpoint.");
        }

        usuario.cambiarEstado(datos);
        return usuario;
    }

    // --- contraseña y bloqueo ---

    @Transactional
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
    }

    @Transactional
    public CredencialesTemporalesDTO resetearPassword(String username) {
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
        return new CredencialesTemporalesDTO(new DetallesUsuarioDTO(usuario), temporal);
    }

    @Transactional
    public Usuario desbloquear(String username) {
        // Busca la cuenta y elimina su bloqueo de acceso.
        var usuario = buscar(username);
        usuario.desbloquear();
        return usuario;
    }

    @Transactional
    public void registrarFalloLogin(String username) {
        // Si la cuenta existe, acumula el intento fallido y aplica el bloqueo configurado.
        usuarioRepository.findByUsername(username)
                .ifPresent(u -> u.registrarFallo(maxIntentos, Duration.ofMinutes(bloqueoMinutos)));
    }

    @Transactional
    public void registrarExitoLogin(String username) {
        // Un inicio de sesión exitoso limpia los contadores de bloqueo.
        usuarioRepository.findByUsername(username).ifPresent(u -> {
            if (u.getIntentosFallidos() > 0 || u.getBloqueadoHasta() != null) {
                u.desbloquear();
            }
        });
    }

    private Usuario buscar(String username) {
        // Centraliza la consulta de usuarios y el error cuando no existe.
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + username));
    }
}