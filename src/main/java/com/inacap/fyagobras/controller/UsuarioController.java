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
// Este controlador administra usuarios, perfiles, contraseñas, roles y estados.
// Cada ResponseEntity describe el resultado HTTP que recibirá el cliente web o móvil.
public class UsuarioController {

    // Inyecta el servicio que contiene la lógica de negocio relacionada con los usuarios
    @Autowired
    private UsuarioService usuarioService;

    /**
     * Registrar un nuevo usuario.
     * @param datos los datos del usuario a registrar
     * @param actor el administrador autenticado que crea la cuenta (para la auditoría)
     * @param uriComponentsBuilder para construir la URI del recurso creado
     * @return la respuesta con el estado 201 Created
     */
    // Restringe el acceso solo a usuarios con el rol ADMIN
    @PreAuthorize("hasRole('ADMIN')")
    // Define un endpoint POST para registrar un nuevo usuario
    @PostMapping
    public ResponseEntity<DetallesUsuarioDTO> registrar(
            @RequestBody @Valid RegistrarUsuarioDTO datos,
            @AuthenticationPrincipal Usuario actor,
            UriComponentsBuilder uriComponentsBuilder) {

        // @RequestBody recibe el JSON y @Valid comprueba sus reglas antes de continuar.
        // actor representa al administrador que crea la cuenta y UriComponentsBuilder construye su URL.
        // Llama al servicio para registrar el usuario (con el actor para auditar) y obtiene sus detalles
        var creado = usuarioService.registrar(datos, actor);
        // Construye la URI absoluta para acceder al recurso creado (/usuarios/{username})
        var uri = uriComponentsBuilder.path("/usuarios/{username}")
                .buildAndExpand(creado.username())
                .toUri();
        // 201 Created indica que la cuenta fue creada.
        // Ubicación apunta al usuario y body contiene sus datos públicos.
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
        // No se recibe un usuario desde la URL: se toma directamente del token autenticado.
        // 200 OK entrega el perfil dentro de un objeto DetallesUsuarioDTO convertido a JSON.
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
    // Restringe el acceso a roles ADMIN, SUPERVISOR, GERENCIA o FINANZAS (Finanzas lo usa para filtrar reportes)
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','GERENCIA','FINANZAS')")
    // Define un endpoint GET en "/usuarios/tecnicos" para obtener una lista simplificada de técnicos
    @GetMapping("/tecnicos")
    public ResponseEntity<List<TecnicoDTO>> tecnicos() {
        // Esta operación devuelve una lista simplificada con ID, nombre y apellido.
        // 200 OK indica consulta exitosa; una lista vacía también es válida.
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
        // @PathVariable extrae el username desde la URL.
        // 200 OK devuelve los detalles si el usuario existe y el permiso fue aprobado.
        // Busca el usuario mediante el nombre de usuario recibido.
        var usuario = usuarioService.obtenerPorUsername(username);
        // Retorna los detalles del usuario encontrado con un estado 200 OK
        return ResponseEntity.ok(new DetallesUsuarioDTO(usuario));
    }

    /**
     * Administrar la cuenta de un usuario (cambiar rol).
     * @param username
     * @param datos
     * @param actor el administrador autenticado que hace el cambio (para la auditoría)
     * @return
     */
    // Restringe el acceso exclusivo a administradores
    @PreAuthorize("hasRole('ADMIN')")
    // Define un endpoint PATCH para modificar el rol o cuenta de un usuario específico
    @PatchMapping("/{username}/cuenta")
    public ResponseEntity<DetallesUsuarioDTO> administrarCuenta(
            @PathVariable String username,
            @RequestBody @Valid ModificarRolDTO datos,
            @AuthenticationPrincipal Usuario actor) {

        // Ejecuta la actualización en el servicio y recibe el usuario modificado
        var usuario = usuarioService.administrarCuenta(username, datos, actor);
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
     * @param actor el administrador autenticado que hace el cambio (para la auditoría)
     * @return
     */
    // Restringe el acceso solo a administradores
    @PreAuthorize("hasRole('ADMIN')")
    // Define un endpoint PATCH para activar o desactivar un usuario
    @PatchMapping("/{username}/estado")
    public ResponseEntity<DetallesUsuarioDTO> cambiarEstado(
            @PathVariable String username,
            @RequestBody @Valid UsuarioActivoDTO datos,
            @AuthenticationPrincipal Usuario actor) {

        // Modifica el estado activo del usuario mediante el servicio
        var usuario = usuarioService.cambiarEstado(username, datos, actor);
        // Retorna el usuario actualizado con estado 200 OK
        return ResponseEntity.ok(new DetallesUsuarioDTO(usuario));
    }

    /**
     * Resetear la contraseña de un usuario y generar credenciales temporales.
     * @param username
     * @param actor el administrador autenticado que hace el reset (para la auditoría)
     * @return
     */
    // Restringe el acceso solo a administradores
    @PreAuthorize("hasRole('ADMIN')")
    // Define un endpoint POST para restablecer la contraseña y generar claves temporales
    @PostMapping("/{username}/reset-password")
    public ResponseEntity<CredencialesTemporalesDTO> resetearPassword(
            @PathVariable String username,
            @AuthenticationPrincipal Usuario actor) {
        // Solicita al servicio el restablecimiento de la contraseña.
        var credenciales = usuarioService.resetearPassword(username, actor);
        // Retorna las credenciales temporales generadas por el servicio con un estado 200 OK
        return ResponseEntity.ok(credenciales);
    }

    /**
     * Desbloquear un usuario bloqueado.
     * @param username
     * @param actor el administrador autenticado que levanta el bloqueo (para la auditoría)
     * @return
     */
    // Restringe el acceso solo a administradores
    @PreAuthorize("hasRole('ADMIN')")
    // Define un endpoint PATCH para desbloquear una cuenta de usuario bloqueada
    @PatchMapping("/{username}/desbloqueo")
    public ResponseEntity<DetallesUsuarioDTO> desbloquear(
            @PathVariable String username,
            @AuthenticationPrincipal Usuario actor) {
        // Ejecuta el desbloqueo a través del servicio
        var usuario = usuarioService.desbloquear(username, actor);
        // Retorna los detalles del usuario desbloqueado con estado 200 OK
        return ResponseEntity.ok(new DetallesUsuarioDTO(usuario));
    }
}
            // actual viene del token y datos contiene la contraseña actual y la nueva.
            // Void significa que el cuerpo de la respuesta estará vacío.
            // 204 No Content confirma el cambio sin enviar datos adicionales.
            // role y activo son filtros opcionales recibidos como parámetros de consulta.
            // Pageable controla página y orden; PagedModel convierte el resultado en una respuesta paginada.
            // 200 OK devuelve la página de usuarios encontrados.
            // El cuerpo contiene el nuevo rol y actor registra quién hizo el cambio.
            // 200 OK entrega la cuenta ya modificada.
            // El nombre de usuario debe coincidir con la identidad autenticada por la regla @PreAuthorize.
            // 200 OK devuelve los datos personales ya actualizados.
            // El cuerpo indica si la cuenta quedará activa o inactiva.
            // actor permite registrar esta acción administrativa.
            // 200 OK devuelve el usuario y su nuevo estado.
            // El servicio genera una contraseña temporal y la devuelve una sola vez.
            // 200 OK entrega las credenciales temporales al administrador autenticado.
            // actor registra quién levantó el bloqueo.
            // 200 OK devuelve el usuario ya habilitado nuevamente.