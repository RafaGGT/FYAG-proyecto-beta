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

// Define la clase como un controlador REST y mapea la ruta base "/usuarios"
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    // Inyecta el servicio que contiene la lógica de negocio relacionada con los usuarios
    @Autowired
    private UsuarioService usuarioService;

    /**
     * Registrar un nuevo usuario.
     * @param datos los datos del usuario a registrar
     * @param uriComponentsBuilder para construir la URI del recurso creado
     * @return la respuesta con el estado 201 Created
     */
    // Restringe el acceso solo a usuarios con el rol ADMIN
    @PreAuthorize("hasRole('ADMIN')")
    // Define un endpoint POST para registrar un nuevo usuario
    @PostMapping
    public ResponseEntity<DetallesUsuarioDTO> registrar(
            @RequestBody @Valid RegistrarUsuarioDTO datos,
            UriComponentsBuilder uriComponentsBuilder) {

        // Llama al servicio para registrar el usuario y obtiene sus detalles
        var creado = usuarioService.registrar(datos);
        // Construye la URI absoluta para acceder al recurso creado (/usuarios/{username})
        var uri = uriComponentsBuilder.path("/usuarios/{username}")
                .buildAndExpand(creado.username())
                .toUri();
        // Retorna una respuesta 201 Created con la URI y el cuerpo con los detalles del usuario
        return ResponseEntity.created(uri).body(creado);
    }
    /**
     * Obtener los detalles del usuario autenticado.
     * @param actual el usuario autenticado
     * @return los detalles del usuario
     */
    // Define un endpoint GET en "/usuarios/me" para obtener el perfil del usuario actual
    @GetMapping("/me")
    public ResponseEntity<DetallesUsuarioDTO> perfil(@AuthenticationPrincipal Usuario actual) {
        // Retorna los detalles mapeados del usuario autenticado actual con un estado 200 OK
        return ResponseEntity.ok(new DetallesUsuarioDTO(actual));
    }

    /**
     * Cambiar la contraseña del usuario autenticado.
     * @param actual
     * @param datos
     * @return
     */
    // Define un endpoint PATCH en "/usuarios/me/password" para cambiar la contraseña propia
    @PatchMapping("/me/password")
    public ResponseEntity<Void> cambiarPassword(
            @AuthenticationPrincipal Usuario actual,
            @RequestBody @Valid CambiarPasswordDTO datos) {

        // Llama al servicio para actualizar la contraseña usando el username del usuario actual
        usuarioService.cambiarPassword(actual.getUsername(), datos);
        // Retorna una respuesta 204 No Content indicando que la operación fue exitosa sin contenido de retorno
        return ResponseEntity.noContent().build();
    }
    /**
     * Listar usuarios.
     * @param role el rol a filtrar
     * @param activo el estado a filtrar
     * @param paginacion la paginación
     * @return la lista de usuarios
     */
    // Restringe el acceso a usuarios con rol ADMIN o GERENCIA
    @PreAuthorize("hasAnyRole('ADMIN','GERENCIA')")
    // Define un endpoint GET para listar usuarios de forma paginada y filtrable
    @GetMapping
    public ResponseEntity<PagedModel<DetallesUsuarioDTO>> listar(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20, sort = "username") Pageable paginacion) {

        // Obtiene la página de usuarios aplicando los filtros y la mapea a DTOs de detalles
        var pagina = usuarioService.listar(role, activo, paginacion).map(DetallesUsuarioDTO::new);
        // Retorna la página envuelta en un PagedModel con estado 200 OK
        return ResponseEntity.ok(new PagedModel<>(pagina));
    }

    /**
     * Listar técnicos.
     * @return
     */
    // Restringe el acceso a roles ADMIN, SUPERVISOR o GERENCIA
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','GERENCIA')")
    // Define un endpoint GET en "/usuarios/tecnicos" para obtener una lista simplificada de técnicos
    @GetMapping("/tecnicos")
    public ResponseEntity<List<TecnicoDTO>> tecnicos() {
        // Obtiene la lista de técnicos del servicio y los mapea a TecnicoDTO
        var lista = usuarioService.listarTecnicos().stream()
                .map(u -> new TecnicoDTO(u.getId(), u.getNombre(), u.getApellido()))
                .toList();
        // Retorna la lista con un estado 200 OK
        return ResponseEntity.ok(lista);
    }

    /**
     * Obtener los detalles de un usuario por su nombre de usuario.
     * @param username
     * @return
     */
    // Permite el acceso si el usuario es ADMIN/GERENCIA o si el username consultado coincide con el token actual
    @PreAuthorize("hasAnyRole('ADMIN','GERENCIA') or #username == authentication.name")
    // Define un endpoint GET para buscar un usuario por su username en la ruta
    @GetMapping("/{username}")
    public ResponseEntity<DetallesUsuarioDTO> obtenerPorUsername(@PathVariable String username) {
        // Busca el usuario mediante el nombre de usuario recibido.
        var usuario = usuarioService.obtenerPorUsername(username);
        // Retorna los detalles del usuario encontrado con un estado 200 OK
        return ResponseEntity.ok(new DetallesUsuarioDTO(usuario));
    }

    /**
     * Administrar la cuenta de un usuario (cambiar rol).
     * @param username
     * @param datos
     * @return
     */
    // Restringe el acceso exclusivo a administradores
    @PreAuthorize("hasRole('ADMIN')")
    // Define un endpoint PATCH para modificar el rol o cuenta de un usuario específico
    @PatchMapping("/{username}/cuenta")
    public ResponseEntity<DetallesUsuarioDTO> administrarCuenta(
            @PathVariable String username,
            @RequestBody @Valid ModificarRolDTO datos) {

        // Ejecuta la actualización en el servicio y recibe el usuario modificado
        var usuario = usuarioService.administrarCuenta(username, datos);
        // Retorna los detalles actualizados del usuario con estado 200 OK
        return ResponseEntity.ok(new DetallesUsuarioDTO(usuario));
    }

    /**
     * Actualizar los datos del usuario autenticado.
     * @param username el nombre de usuario
     * @param datos los datos a actualizar
     * @return el usuario actualizado
     */
    // Permite la actualización únicamente si el usuario que edita coincide con el propietario de la cuenta
    @PreAuthorize("#username == authentication.name")
    // Define un endpoint PUT para actualizar los datos personales de un usuario
    @PutMapping("/{username}")
    public ResponseEntity<DetallesUsuarioDTO> actualizarDatos(
            @PathVariable String username,
            @RequestBody @Valid ModificarUsuarioDTO datos) {

        // Llama al servicio para actualizar los datos y obtiene la entidad resultante
        var usuario = usuarioService.actualizarDatos(username, datos);
        // Retorna los nuevos detalles del usuario con estado 200 OK
        return ResponseEntity.ok(new DetallesUsuarioDTO(usuario));
    }

    /**
     * Cambiar el estado activo de un usuario.
     * @param username
     * @param datos
     * @return
     */
    // Restringe el acceso solo a administradores
    @PreAuthorize("hasRole('ADMIN')")
    // Define un endpoint PATCH para activar o desactivar un usuario
    @PatchMapping("/{username}/estado")
    public ResponseEntity<DetallesUsuarioDTO> cambiarEstado(
            @PathVariable String username,
            @RequestBody @Valid UsuarioActivoDTO datos) {

        // Modifica el estado activo del usuario mediante el servicio
        var usuario = usuarioService.cambiarEstado(username, datos);
        // Retorna el usuario actualizado con estado 200 OK
        return ResponseEntity.ok(new DetallesUsuarioDTO(usuario));
    }

    /**
     * Resetear la contraseña de un usuario y generar credenciales temporales.
     * @param username
     * @return
     */
    // Restringe el acceso solo a administradores
    @PreAuthorize("hasRole('ADMIN')")
    // Define un endpoint POST para restablecer la contraseña y generar claves temporales
    @PostMapping("/{username}/reset-password")
    public ResponseEntity<CredencialesTemporalesDTO> resetearPassword(@PathVariable String username) {
        // Solicita al servicio el restablecimiento de la contraseña.
        var credenciales = usuarioService.resetearPassword(username);
        // Retorna las credenciales temporales generadas por el servicio con un estado 200 OK
        return ResponseEntity.ok(credenciales);
    }

    /**
     * Desbloquear un usuario bloqueado.
     * @param username
     * @return
     */
    // Restringe el acceso solo a administradores
    @PreAuthorize("hasRole('ADMIN')")
    // Define un endpoint PATCH para desbloquear una cuenta de usuario bloqueada
    @PatchMapping("/{username}/desbloqueo")
    public ResponseEntity<DetallesUsuarioDTO> desbloquear(@PathVariable String username) {
        // Ejecuta el desbloqueo a través del servicio
        var usuario = usuarioService.desbloquear(username);
        // Retorna los detalles del usuario desbloqueado con estado 200 OK
        return ResponseEntity.ok(new DetallesUsuarioDTO(usuario));
    }
}