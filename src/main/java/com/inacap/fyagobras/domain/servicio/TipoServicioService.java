package com.inacap.fyagobras.domain.servicio;

import com.inacap.fyagobras.domain.servicio.dto.*;
import com.inacap.fyagobras.domain.servicio.repository.TipoServicioRepository;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TipoServicioService {

    @Autowired
    private TipoServicioRepository repository;

    /** Lista para el desplegable: solo vigentes. Con incluirInactivos, el panel del Supervisor ve todos. */
    @Transactional(readOnly = true)
    public List<DetallesTipoServicioDTO> listar(boolean incluirInactivos) {
        var tipos = incluirInactivos
                ? repository.findAllByOrderByNombreAsc()
                : repository.findByActivoTrueOrderByNombreAsc();
        return tipos.stream().map(DetallesTipoServicioDTO::new).toList();
    }

    @Transactional
    public DetallesTipoServicioDTO registrar(RegistrarTipoServicioDTO datos) {
        var nombre = datos.nombre().trim();
        if (repository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoNegocioException("Ya existe un tipo de servicio con ese nombre.");
        }
        return new DetallesTipoServicioDTO(repository.save(new TipoServicio(nombre)));
    }

    @Transactional
    public DetallesTipoServicioDTO modificar(Long id, ModificarTipoServicioDTO datos) {
        var tipo = buscar(id);
        var nombre = datos.nombre().trim();
        if (repository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictoNegocioException("Ya existe un tipo de servicio con ese nombre.");
        }
        tipo.renombrar(nombre);
        return new DetallesTipoServicioDTO(tipo);
    }

    @Transactional
    public DetallesTipoServicioDTO cambiarEstado(Long id, CambiarEstadoTipoServicioDTO datos) {
        var tipo = buscar(id);
        tipo.cambiarEstado(datos.activo());
        return new DetallesTipoServicioDTO(tipo);
    }

    private TipoServicio buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de servicio no encontrado"));
    }
}