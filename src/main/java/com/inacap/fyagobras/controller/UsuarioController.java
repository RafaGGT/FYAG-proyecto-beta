package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import com.inacap.fyagobras.domain.user.UsuarioService;
import com.inacap.fyagobras.domain.user.dto.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Registrar un nuevo usuario.
     * @param datos los datos del usuario a registrar
     * @param uriComponentsBuilder para construir la URI del recurso creado
     * @return la respuesta con el estado 201 Created y el cuerpo con las credenciales temporales
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CredencialesTemporalesDTO> registrar(
            @RequestBody @Valid RegistrarUsuarioDTO datos,
            UriComponentsBuilder uriComponentsBuilder) {

        var creado = usuarioService.registrar(datos);
        var uri = uriComponentsBuilder.path("/usuarios/{username}")
                .buildAndExpand(creado.usuario().username())
                .toUri();
        return ResponseEntity.created(uri).body(creado);
    }
    /**
     * Obtener los detalles del usuario autenticado.
     * @param actual el usuario autenticado
     * @return los detalles del usuario
     */
    @GetMapping("/me")
    public ResponseEntity<DetallesUsuarioDTO> perfil(@AuthenticationPrincipal Usuario actual) {
        return ResponseEntity.ok(new DetallesUsuarioDTO(actual));
    }

    /**
     * Cambiar la contraseña del usuario autenticado.
     * @param actual
     * @param datos
     * @return
     */
    @PatchMapping("/me/password")
    public ResponseEntity<Void> cambiarPassword(
            @AuthenticationPrincipal Usuario actual,
            @RequestBody @Valid CambiarPasswordDTO datos) {

        usuarioService.cambiarPassword(actual.getUsername(), datos);
        return ResponseEntity.noContent().build();
    }
    /**
     * Listar usuarios.
     * @param role el rol a filtrar
     * @param activo el estado a filtrar
     * @param paginacion la paginación
     * @return la lista de usuarios
     */
    @PreAuthorize("hasAnyRole('ADMIN','GERENCIA')")
    @GetMapping
    public ResponseEntity<PagedModel<DetallesUsuarioDTO>> listar(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20, sort = "username") Pageable paginacion) {

        var pagina = usuarioService.listar(role, activo, paginacion).map(DetallesUsuarioDTO::new);
        return ResponseEntity.ok(new PagedModel<>(pagina));
    }

    /**
     * Listar técnicos.
     * @return
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','GERENCIA')")
    @GetMapping("/tecnicos")
    public ResponseEntity<List<TecnicoDTO>> tecnicos() {
        var lista = usuarioService.listarTecnicos().stream()
                .map(u -> new TecnicoDTO(u.getId(), u.getNombre(), u.getApellido()))
                .toList();
        return ResponseEntity.ok(lista);
    }

    /**
     * Obtener los detalles de un usuario por su nombre de usuario.
     * @param username
     * @return
     */
    @PreAuthorize("hasAnyRole('ADMIN','GERENCIA') or #username == authentication.name")
    @GetMapping("/{username}")
    public ResponseEntity<DetallesUsuarioDTO> obtenerPorUsername(@PathVariable String username) {    var usuario = usuarioService.obtenerPorUsername(username);
        return ResponseEntity.ok(new DetallesUsuarioDTO(usuario));
    }

    /**
     * Administrar la cuenta de un usuario (cambiar rol).
     * @param username
     * @param datos
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{username}/cuenta")
    public ResponseEntity<DetallesUsuarioDTO> administrarCuenta(
            @PathVariable String username,
            @RequestBody @Valid ModificarRolDTO datos) {

        var usuario = usuarioService.administrarCuenta(username, datos);
        return ResponseEntity.ok(new DetallesUsuarioDTO(usuario));
    }

    /**
     * Actualizar los datos del usuario autenticado.
     * @param username el nombre de usuario
     * @param datos los datos a actualizar
     * @return el usuario actualizado
     */
    @PreAuthorize("#username == authentication.name")
    @PutMapping("/{username}")
    public ResponseEntity<DetallesUsuarioDTO> actualizarDatos(
            @PathVariable String username,
            @RequestBody @Valid ModificarUsuarioDTO datos) {

        var usuario = usuarioService.actualizarDatos(username, datos);
        return ResponseEntity.ok(new DetallesUsuarioDTO(usuario));
    }

    /**
     * Cambiar el estado activo de un usuario.
     * @param username
     * @param datos
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{username}/estado")
    public ResponseEntity<DetallesUsuarioDTO> cambiarEstado(
            @PathVariable String username,
            @RequestBody @Valid UsuarioActivoDTO datos) {

        var usuario = usuarioService.cambiarEstado(username, datos);
        return ResponseEntity.ok(new DetallesUsuarioDTO(usuario));
    }

    /**
     * Resetear la contraseña de un usuario y generar credenciales temporales.
     * @param username
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{username}/reset-password")
    public ResponseEntity<CredencialesTemporalesDTO> resetearPassword(@PathVariable String username) {
        return ResponseEntity.ok(usuarioService.resetearPassword(username));
    }

    /**
     * Desbloquear un usuario bloqueado.
     * @param username
     * @return
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{username}/desbloqueo")
    public ResponseEntity<DetallesUsuarioDTO> desbloquear(@PathVariable String username) {
        var usuario = usuarioService.desbloquear(username);
        return ResponseEntity.ok(new DetallesUsuarioDTO(usuario));
    }
}