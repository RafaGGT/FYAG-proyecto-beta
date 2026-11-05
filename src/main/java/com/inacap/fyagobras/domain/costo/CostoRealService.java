package com.inacap.fyagobras.domain.costo;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.costo.dto.DetallesCostosDTO;
import com.inacap.fyagobras.domain.costo.dto.RegistrarCostosDTO;
import com.inacap.fyagobras.domain.costo.dto.RentabilidadDTO;
import com.inacap.fyagobras.domain.costo.repository.CostoRealRepository;
import com.inacap.fyagobras.domain.obra.Obra;
import com.inacap.fyagobras.domain.obra.repository.ObraRepository;
import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class CostoRealService {

    @Autowired private CostoRealRepository costoRepository;
    @Autowired private ObraRepository obraRepository;
    @Autowired private AuditoriaService auditoriaService;

    /** HU-04: registra (o reemplaza) los costos reales de la obra. Un registro por obra. */
    @Transactional
    public DetallesCostosDTO registrar(Long obraId, RegistrarCostosDTO datos, Usuario actor) {
        var obra = buscarObra(obraId, actor);
        var existente = costoRepository.findByObraId(obraId);

        Map<String, Object> anterior = null;
        CostoReal costo;
        String accion;
        if (existente.isPresent()) {
            costo = existente.get();
            anterior = mapa(costo);
            costo.actualizar(datos.materiales(), datos.horasTecnicas(), datos.valorHora());
            accion = "ACTUALIZAR_COSTOS";
        } else {
            costo = costoRepository.save(new CostoReal(obra,
                    datos.materiales(), datos.horasTecnicas(), datos.valorHora()));
            accion = "REGISTRAR_COSTOS";
        }

        auditoriaService.registrar(actor, "costos_reales", costo.getId(), accion,
                null, anterior, mapa(costo));

        return new DetallesCostosDTO(costo);
    }

    @Transactional(readOnly = true)
    public DetallesCostosDTO obtener(Long obraId, Usuario actor) {
        buscarObra(obraId, actor);
        var costo = costoRepository.findByObraId(obraId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La obra aún no tiene costos registrados"));
        return new DetallesCostosDTO(costo);
    }

    /** HU-04: diferencia entre el presupuesto aprobado y el costo real. */
    @Transactional(readOnly = true)
    public RentabilidadDTO rentabilidad(Long obraId) {
        var obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
        var costo = costoRepository.findByObraId(obraId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La obra aún no tiene costos registrados"));

        var presupuesto = obra.getPresupuesto().getMontoTotal();
        var total = costo.costoTotal();

        return new RentabilidadDTO(obraId, presupuesto, costo.getMateriales(),
                costo.costoManoObra(), total, presupuesto.subtract(total));
    }

    /** El Técnico solo opera sobre las obras que tiene asignadas (HU-24). */
    private Obra buscarObra(Long obraId, Usuario actor) {
        var obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
        boolean esTecnico = actor.getRole() == Role.TECNICO;
        boolean esSuya = obra.getTecnico() != null && obra.getTecnico().getId().equals(actor.getId());
        if (esTecnico && !esSuya) {
            throw new AccessDeniedException("Solo puede operar sobre las obras que tiene asignadas.");
        }
        return obra;
    }

    private Map<String, Object> mapa(CostoReal c) {
        return Map.of(
                "materiales", c.getMateriales().toPlainString(),
                "horas_tecnicas", c.getHorasTecnicas().toPlainString(),
                "valor_hora", c.getValorHora().toPlainString());
    }
}