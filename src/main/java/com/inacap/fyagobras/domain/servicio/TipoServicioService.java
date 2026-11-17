package com.inacap.fyagobras.domain.servicio;

import com.inacap.fyagobras.domain.servicio.dto.*;
import com.inacap.fyagobras.domain.servicio.repository.TipoServicioRepository;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.user.Usuario;
import java.util.Map;
import java.util.List;

@Service
public class TipoServicioService {

    @Autowired
    private TipoServicioRepository repository;

    @Autowired
    private AuditoriaService auditoriaService;

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
    public DetallesTipoServicioDTO registrar(RegistrarTipoServicioDTO datos, Usuario actor) {
        var nombre = datos.nombre().trim();
        if (repository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoNegocioException("Ya existe un tipo de servicio con ese nombre.");
        }
        var guardado = repository.save(new TipoServicio(nombre));
        // Auditoría: alta de un tipo de servicio (HU-08)
        auditoriaService.registrar(actor, "tipos_servicio", guardado.getId(), "CREAR_TIPO_SERVICIO", null, null,
                Map.of("nombre", guardado.getNombre(), "activo", guardado.isActivo()));
        return new DetallesTipoServicioDTO(guardado);
    }

    @Transactional
    public DetallesTipoServicioDTO modificar(Long id, ModificarTipoServicioDTO datos, Usuario actor) {
        var tipo = buscar(id);
        var nombre = datos.nombre().trim();
        if (repository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictoNegocioException("Ya existe un tipo de servicio con ese nombre.");
        }
        // Nombre anterior antes de renombrar
        var anterior = tipo.getNombre();
        tipo.renombrar(nombre);
        if (!anterior.equals(nombre)) {
            auditoriaService.registrar(actor, "tipos_servicio", tipo.getId(), "MODIFICAR_TIPO_SERVICIO", null,
                    Map.of("nombre", anterior), Map.of("nombre", nombre));
        }
        return new DetallesTipoServicioDTO(tipo);
    }

    @Transactional
    public DetallesTipoServicioDTO cambiarEstado(Long id, CambiarEstadoTipoServicioDTO datos, Usuario actor) {
        var tipo = buscar(id);
        var anterior = tipo.isActivo();
        tipo.cambiarEstado(datos.activo());
        // Auditoría: se desactiva, nunca se elimina
        if (anterior != datos.activo()) {
            auditoriaService.registrar(actor, "tipos_servicio", tipo.getId(), "CAMBIAR_ESTADO_TIPO_SERVICIO", null,
                    Map.of("activo", anterior), Map.of("activo", datos.activo()));
        }
        return new DetallesTipoServicioDTO(tipo);
    }
    private TipoServicio buscar(Long id) {
        // Centraliza la búsqueda y el error para los métodos del servicio.
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tipo de servicio no encontrado"));
    }
}