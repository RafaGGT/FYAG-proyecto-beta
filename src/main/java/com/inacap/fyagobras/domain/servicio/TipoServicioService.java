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
        // Selecciona la consulta según el nivel de detalle solicitado.
        var tipos = incluirInactivos
                ? repository.findAllByOrderByNombreAsc()
                : repository.findByActivoTrueOrderByNombreAsc();
        // Expone los tipos como DTOs y no como entidades persistentes.
        return tipos.stream().map(DetallesTipoServicioDTO::new).toList();
    }

    @Transactional
    public DetallesTipoServicioDTO registrar(RegistrarTipoServicioDTO datos) {
        // Normaliza el nombre antes de validar y guardar.
        var nombre = datos.nombre().trim();
        // Impide registrar dos tipos con el mismo nombre.
        if (repository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoNegocioException("Ya existe un tipo de servicio con ese nombre.");
        }
        // Persiste el nuevo tipo y devuelve su representación de salida.
        return new DetallesTipoServicioDTO(repository.save(new TipoServicio(nombre)));
    }

    @Transactional
    public DetallesTipoServicioDTO modificar(Long id, ModificarTipoServicioDTO datos) {
        // Recupera el tipo que se desea modificar.
        var tipo = buscar(id);
        var nombre = datos.nombre().trim();
        // Valida la unicidad ignorando el registro actualmente editado.
        if (repository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictoNegocioException("Ya existe un tipo de servicio con ese nombre.");
        }
        // Cambia el nombre dentro de la entidad administrada por JPA.
        tipo.renombrar(nombre);
        return new DetallesTipoServicioDTO(tipo);
    }

    @Transactional
    public DetallesTipoServicioDTO cambiarEstado(Long id, CambiarEstadoTipoServicioDTO datos) {
        // Obtiene el tipo y aplica el nuevo estado activo/inactivo.
        var tipo = buscar(id);
        tipo.cambiarEstado(datos.activo());
        return new DetallesTipoServicioDTO(tipo);
    }

    private TipoServicio buscar(Long id) {
        // Centraliza la búsqueda y el error para los métodos del servicio.
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de servicio no encontrado"));
    }
}