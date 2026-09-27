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
    public CredencialesTemporalesDTO registrar(RegistrarUsuarioDTO datos) {
        crearValidador.forEach(v -> v.validate(datos));
        var temporal = GeneradorPassword.generar(12);
        var usuario = new Usuario(datos, passwordEncoder.encode(temporal));
        usuario.setDebeCambiarPassword(true);
        var guardado = usuarioRepository.save(usuario);
        return new CredencialesTemporalesDTO(new DetallesUsuarioDTO(guardado), temporal);
    }

    public Usuario obtenerPorUsername(String username) {
        return buscar(username);
    }

    public Page<Usuario> listar(Role role, Boolean activo, Pageable paginacion) {
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
        return usuarioRepository.findByRoleAndActivoTrueOrderByApellidoAsc(Role.TECNICO);
    }

    @Transactional
    public Usuario administrarCuenta(String username, ModificarRolDTO datos) {
        var usuario = buscar(username);

        if (usuario.getRole() == Role.ADMIN || datos.role() == Role.ADMIN) {
            throw new ConflictoNegocioException(
                    "El rol ADMIN es intocable: no se puede asignar ni modificar desde este endpoint.");
        }

        usuario.actualizarCuenta(datos);
        return usuario;
    }

    @Transactional
    public Usuario actualizarDatos(String username, ModificarUsuarioDTO datos) {
        actualizarValidador.forEach(v -> v.validate(username, datos));
        var usuario = buscar(username);

        usuario.actualizarUsuario(datos);
        return usuario;
    }

    @Transactional
    public Usuario despedir(String username) {
        var usuario = buscar(username);

        if (usuario.getRole() == Role.ADMIN) {
            throw new ConflictoNegocioException(
                    "El rol ADMIN es intocable: no se puede desactivar desde este endpoint.");
        }

        if (!usuario.isEnabled()) {
            throw new ConflictoNegocioException("El usuario ya se encuentra desactivado.");
        }

        usuario.desactivar();
        return usuario;
    }

    @Transactional
    public Usuario cambiarEstado(String username, UsuarioActivoDTO datos) {
        var usuario = buscar(username);

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
        var usuario = buscar(username);

        if (!passwordEncoder.matches(datos.passwordActual(), usuario.getPassword())) {
            throw new ConflictoNegocioException("La contraseña actual es incorrecta.");
        }
        if (passwordEncoder.matches(datos.passwordNueva(), usuario.getPassword())) {
            throw new ConflictoNegocioException("La nueva contraseña debe ser distinta a la actual.");
        }

        usuario.cambiarPassword(passwordEncoder.encode(datos.passwordNueva()));
    }

    @Transactional
    public CredencialesTemporalesDTO resetearPassword(String username) {
        var usuario = buscar(username);

        if (usuario.getRole() == Role.ADMIN) {
            throw new ConflictoNegocioException(
                    "El rol ADMIN es intocable: no se puede resetear su contraseña desde este endpoint.");
        }

        var temporal = GeneradorPassword.generar(12);
        usuario.asignarPasswordTemporal(passwordEncoder.encode(temporal));
        return new CredencialesTemporalesDTO(new DetallesUsuarioDTO(usuario), temporal);
    }

    @Transactional
    public Usuario desbloquear(String username) {
        var usuario = buscar(username);
        usuario.desbloquear();
        return usuario;
    }

    @Transactional
    public void registrarFalloLogin(String username) {
        usuarioRepository.findByUsername(username)
                .ifPresent(u -> u.registrarFallo(maxIntentos, Duration.ofMinutes(bloqueoMinutos)));
    }

    @Transactional
    public void registrarExitoLogin(String username) {
        usuarioRepository.findByUsername(username).ifPresent(u -> {
            if (u.getIntentosFallidos() > 0 || u.getBloqueadoHasta() != null) {
                u.desbloquear();
            }
        });
    }

    private Usuario buscar(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + username));
    }
}