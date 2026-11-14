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

import java.util.List;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private List<ValidarCrearCliente> crearValidador;

    @Transactional
    public DetallesClienteDTO registrar(RegistrarClienteDTO datos) {
        // Ejecuta todas las validaciones registradas para la creación.
        crearValidador.forEach(v -> v.validate(datos));
        // Crea y persiste la entidad a partir de los datos recibidos.
        var guardado = clienteRepository.save(new Cliente(datos));
        // Devuelve únicamente la información pública del cliente.
        return new DetallesClienteDTO(guardado);
    }

    public DetallesClienteDTO obtener(Long id) {
        // Busca el cliente y transforma la entidad en un DTO de respuesta.
        return new DetallesClienteDTO(buscar(id));
    }

    public Page<DetallesClienteDTO> listar(String busqueda, Pageable paginacion) {
        // Usa la consulta general cuando no se solicitó un filtro.
        var pagina = (busqueda == null || busqueda.isBlank())
                ? clienteRepository.findAll(paginacion)
                : clienteRepository.buscar(busqueda.trim(), paginacion);
        // Convierte cada entidad de la página a su DTO correspondiente.
        return pagina.map(DetallesClienteDTO::new);
    }

    @Transactional
    public DetallesClienteDTO actualizar(Long id, ModificarClienteDTO datos) {
        // Obtiene el cliente antes de modificarlo.
        var cliente = buscar(id);
        // Aplica solo los campos permitidos por el dominio.
        cliente.actualizar(datos);   // JPA guarda solo al cerrar la transacción
        // Devuelve el estado actualizado.
        return new DetallesClienteDTO(cliente);
    }

    private Cliente buscar(Long id) {
        // Consulta el repositorio y comunica explícitamente si no existe.
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
    }
}