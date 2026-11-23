package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.cliente.ClienteService;
import com.inacap.fyagobras.domain.cliente.dto.DetallesClienteDTO;
import com.inacap.fyagobras.domain.cliente.dto.ModificarClienteDTO;
import com.inacap.fyagobras.domain.cliente.dto.RegistrarClienteDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import com.inacap.fyagobras.domain.user.Usuario;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

// Define la clase como un controlador REST y mapea la ruta base "/clientes"
@RestController
@RequestMapping("/clientes")
// Este controlador administra el alta y consulta de clientes.
// Traduce peticiones HTTP en llamadas al ClienteService y devuelve resultados mediante ResponseEntity.
public class ClienteController {

    // Inyecta el servicio que contiene la lógica de negocio para la gestión de clientes
    @Autowired
    private ClienteService clienteService;

    /** Registrar un cliente. Solo Finanzas. */
    // Restringe el acceso exclusivo al rol FINANZAS
    @PreAuthorize("hasRole('FINANZAS')")
    // Define un endpoint POST para registrar un nuevo cliente
    @PostMapping
    public ResponseEntity<DetallesClienteDTO> registrar(
            @RequestBody @Valid RegistrarClienteDTO datos,
            @AuthenticationPrincipal Usuario actor,
            UriComponentsBuilder uriComponentsBuilder) {
        // @RequestBody recibe los datos JSON y @Valid activa las validaciones del DTO.
        // actor identifica al usuario FINANZAS que realiza el registro.
        var creado = clienteService.registrar(datos, actor);
        // Construye la URI del recurso recién creado (/clientes/{id})
        var uri = uriComponentsBuilder.path("/clientes/{id}").buildAndExpand(creado.id()).toUri();
        // 201 Created indica creación exitosa.
        // La cabecera Ubicación contiene la dirección del nuevo recurso y body entrega sus datos.
        return ResponseEntity.created(uri).body(creado);
    }

    /** Listar o buscar clientes por RUT o razón social (?busqueda=texto). */
    // Restringe el acceso a los roles FINANZAS, GERENCIA o SUPERVISOR
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA','SUPERVISOR')")
    // Define un endpoint GET para listar o buscar clientes de forma paginada
    @GetMapping
    public ResponseEntity<PagedModel<DetallesClienteDTO>> listar(
            @RequestParam(required = false) String busqueda,
            @PageableDefault(size = 20, sort = "razonSocial") Pageable paginacion) {

        // Consulta los clientes aplicando búsqueda y paginación.
        var clientes = clienteService.listar(busqueda, paginacion);
        // Retorna la página de clientes filtrada y envuelta en un PagedModel con estado 200 OK
        return ResponseEntity.ok(new PagedModel<>(clientes));
    }

    /** Ver el detalle de un cliente. */
    // Restringe el acceso a los roles FINANZAS, GERENCIA o SUPERVISOR
    @PreAuthorize("hasAnyRole('FINANZAS','GERENCIA','SUPERVISOR')")
    // Define un endpoint GET para consultar los detalles de un cliente específico por su ID
    @GetMapping("/{id}")
    public ResponseEntity<DetallesClienteDTO> detalle(@PathVariable Long id) {
        // @PathVariable toma el ID desde la URL.
        // 200 OK entrega el cliente encontrado dentro del cuerpo JSON.
        // Busca el cliente solicitado por su identificador.
        var cliente = clienteService.obtener(id);
        // Retorna los detalles del cliente obtenido con estado 200 OK
        return ResponseEntity.ok(cliente);
    }

    /** Editar datos de contacto. El RUT no se puede cambiar. Solo Finanzas. */
    // Restringe el acceso exclusivo al rol FINANZAS
    @PreAuthorize("hasRole('FINANZAS')")
    // Define un endpoint PUT para actualizar los datos de contacto de un cliente existente por su ID
    @PutMapping("/{id}")
    public ResponseEntity<DetallesClienteDTO> actualizar(
            @PathVariable Long id,
            @RequestBody @Valid ModificarClienteDTO datos,
            @AuthenticationPrincipal Usuario actor) {
        var cliente = clienteService.actualizar(id, datos, actor);
        // Actualiza el cliente a través del servicio y retorna el resultado con estado 200 OK
        return ResponseEntity.ok(cliente);
    }
}
        // busqueda es opcional: si existe filtra por RUT o razón social.
        // Pageable define tamaño y orden; PagedModel incluye datos y metadatos de paginación.
        // 200 OK indica que la consulta fue exitosa.
            // @RequestBody contiene únicamente los datos de contacto permitidos.
            // @Valid verifica el formato antes de llamar al servicio.
            // 200 OK indica modificación exitosa y devuelve el cliente actualizado.