package com.inacap.fyagobras.domain.cliente;

import com.inacap.fyagobras.domain.cliente.dto.DetallesClienteDTO;
import com.inacap.fyagobras.domain.cliente.dto.ModificarClienteDTO;
import com.inacap.fyagobras.domain.cliente.dto.RegistrarClienteDTO;
import com.inacap.fyagobras.domain.cliente.repository.ClienteRepository;
import com.inacap.fyagobras.domain.cliente.validation.create.ValidarCrearCliente;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.user.Usuario;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.List;

@Service
// Clase ClienteService: implementa la funcionalidad del dominio.
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private List<ValidarCrearCliente> crearValidador;
    @Autowired
    private AuditoriaService auditoriaService;

    @Transactional
    // Ejecuta la operacion registrar.
    public DetallesClienteDTO registrar(RegistrarClienteDTO datos) {
        // Ejecuta todas las validaciones registradas para la creación.
        crearValidador.forEach(v -> v.validate(datos));
        // Crea y persiste la entidad a partir de los datos recibidos.
        var guardado = clienteRepository.save(new Cliente(datos));
        // Devuelve únicamente la información pública del cliente.
        return new DetallesClienteDTO(guardado);
    }

    // Ejecuta la operacion obtener.
    public DetallesClienteDTO obtener(Long id) {
        // Busca el cliente y transforma la entidad en un DTO de respuesta.
        return new DetallesClienteDTO(buscar(id));
    }

    // Ejecuta la operacion listar.
    public Page<DetallesClienteDTO> listar(String busqueda, Pageable paginacion) {
        // Usa la consulta general cuando no se solicitó un filtro.
        var pagina = (busqueda == null || busqueda.isBlank())
                ? clienteRepository.findAll(paginacion)
                : clienteRepository.buscar(busqueda.trim(), paginacion);
        // Convierte cada entidad de la página a su DTO correspondiente.
        return pagina.map(DetallesClienteDTO::new);
    }

    @Transactional
    // Ejecuta la operacion actualizar.
    public DetallesClienteDTO actualizar(Long id, ModificarClienteDTO datos) {
        // Obtiene el cliente antes de modificarlo.
        var cliente = buscar(id);
        // Aplica solo los campos permitidos por el dominio.
        cliente.actualizar(datos);   // JPA guarda solo al cerrar la transacción
        // Devuelve el estado actualizado.
        return new DetallesClienteDTO(cliente);
    }

    // Ejecuta la operacion buscar.
    private Cliente buscar(Long id) {
        // Consulta el repositorio y comunica explícitamente si no existe.
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
    }

    @Transactional
    // Ejecuta la operacion registrar.
    public DetallesClienteDTO registrar(RegistrarClienteDTO datos, Usuario actor) {
        crearValidador.forEach(v -> v.validate(datos));
        var guardado = clienteRepository.save(new Cliente(datos));
        // Auditoría: alta de cliente con los datos guardados
        auditoriaService.registrar(actor, "clientes", guardado.getId(), "CREAR_CLIENTE", null, null,
                datosCliente(guardado));
        return new DetallesClienteDTO(guardado);
    }

    @Transactional
    // Ejecuta la operacion actualizar.
    public DetallesClienteDTO actualizar(Long id, ModificarClienteDTO datos, Usuario actor) {
        var cliente = buscar(id);
        // Foto de los datos ANTES de modificar
        var anterior = datosCliente(cliente);
        cliente.actualizar(datos);
        var nuevo = datosCliente(cliente);
        // Si no cambió ningún dato no se registra auditoría
        if (!anterior.equals(nuevo)) {
            auditoriaService.registrar(actor, "clientes", cliente.getId(), "ACTUALIZAR_CLIENTE", null,
                    anterior, nuevo);
        }
        return new DetallesClienteDTO(cliente);
    }

    // LinkedHashMap en vez de Map.of porque dirección, correo y teléfono pueden ser null
    private Map<String, Object> datosCliente(Cliente c) {
        // Crea un mapa ordenado con los datos relevantes del cliente para auditoría.
        var datos = new LinkedHashMap<String, Object>();
        // Agrega cada propiedad del cliente al mapa con su nombre como clave.
        datos.put("rut", c.getRut());
        datos.put("razon_social", c.getRazonSocial());
        datos.put("direccion", c.getDireccion());
        datos.put("email", c.getEmail());
        datos.put("telefono", c.getTelefono());
        return datos;
    }
}
