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
        crearValidador.forEach(v -> v.validate(datos));
        var guardado = clienteRepository.save(new Cliente(datos));
        return new DetallesClienteDTO(guardado);
    }

    public DetallesClienteDTO obtener(Long id) {
        return new DetallesClienteDTO(buscar(id));
    }

    public Page<DetallesClienteDTO> listar(String busqueda, Pageable paginacion) {
        var pagina = (busqueda == null || busqueda.isBlank())
                ? clienteRepository.findAll(paginacion)
                : clienteRepository.buscar(busqueda.trim(), paginacion);
        return pagina.map(DetallesClienteDTO::new);
    }

    @Transactional
    public DetallesClienteDTO actualizar(Long id, ModificarClienteDTO datos) {
        var cliente = buscar(id);
        cliente.actualizar(datos);   // JPA guarda solo al cerrar la transacción
        return new DetallesClienteDTO(cliente);
    }

    private Cliente buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
    }
}