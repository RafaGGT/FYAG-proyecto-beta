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
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
@Service
// Clase CostoRealService: implementa la funcionalidad del dominio.
public class CostoRealService {

    @Autowired private CostoRealRepository costoRepository;
    @Autowired private ObraRepository obraRepository;
    @Autowired private AuditoriaService auditoriaService;

    /** HU-04: registra (o reemplaza) los costos reales de la obra. Un registro por obra. */
    @Transactional
    // Ejecuta la operacion registrar.
    public DetallesCostosDTO registrar(Long obraId, RegistrarCostosDTO datos, Usuario actor) {
        // Recupera la obra y valida que el actor tenga permiso para operar sobre ella.
        var obra = buscarObra(obraId, actor);
        // Recupera el registro de costos existente, si lo hay.
        var existente = costoRepository.findByObraId(obraId);

        // El valor hora es un monto: solo lo define el Supervisor.
        // Si lo envía el Técnico, o no viene, se conserva el existente (o 0 la primera vez).
        BigDecimal valorHora;
        // Si el actor es un Técnico o no se proporcionó valorHora, se mantiene el valor existente o se inicializa en cero.
        if (actor.getRole() == Role.TECNICO || datos.valorHora() == null) {
            // Si existe un registro previo, se conserva su valorHora; de lo contrario, se inicializa en cero.
            valorHora = existente.map(CostoReal::getValorHora).orElse(BigDecimal.ZERO);
            // Si el actor es un Supervisor y se proporcionó valorHora, se utiliza el valor enviado.
        } else {
            valorHora = datos.valorHora();
        }

        Map<String, Object> anterior = null;
        CostoReal costo;
        String accion;
        if (existente.isPresent()) {
            costo = existente.get();
            // Si no cambió ningún valor, no se actualiza ni se registra auditoría
            if (costo.getMateriales().compareTo(datos.materiales()) == 0
                    && costo.getHorasTecnicas().compareTo(datos.horasTecnicas()) == 0
                    && costo.getValorHora().compareTo(valorHora) == 0) {
                return DetallesCostosDTO.para(costo, actor.getRole());
            }
            anterior = mapa(costo);
            costo.actualizar(datos.materiales(), datos.horasTecnicas(), valorHora);
            accion = "ACTUALIZAR_COSTOS";
        } else {
            costo = costoRepository.save(new CostoReal(obra, datos.materiales(), datos.horasTecnicas(), valorHora));
            accion = "REGISTRAR_COSTOS";
        }

        auditoriaService.registrar(actor, "costos_reales", costo.getId(), accion, null, anterior, mapa(costo));

        // La respuesta se adapta al rol: el Técnico no recibe el valor hora
        return DetallesCostosDTO.para(costo, actor.getRole());
    }

    @Transactional(readOnly = true)
    // Ejecuta la operacion obtener.
    public Optional<DetallesCostosDTO> obtener(Long obraId, Usuario actor) {
        buscarObra(obraId, actor);
        // Cierra el hueco por API (HU-25): aunque el front lo oculte, el backend no lo entrega al Técnico
        return costoRepository.findByObraId(obraId)
                .map(costo -> DetallesCostosDTO.para(costo, actor.getRole()));
    }

    /** HU-04: diferencia entre el presupuesto aprobado y el costo real. Vacío si la obra aún no tiene costos. */
    @Transactional(readOnly = true)
    // Ejecuta la operacion rentabilidad.
    public Optional<RentabilidadDTO> rentabilidad(Long obraId) {
        // Recupera la obra para comparar su presupuesto con el costo ejecutado.
        var obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
        // Recupera los costos reales de la obra y calcula la rentabilidad
        return costoRepository
        // findByObraId devuelve un Optional<CostoReal> que se mapea a RentabilidadDTO si está presente
            .findByObraId(obraId).map(costo -> {
                // Calcula la rentabilidad: diferencia entre lo presupuestado y lo ejecutado
            var presupuesto = obra.getPresupuesto().getMontoTotal();
            // Calcula el costo total ejecutado sumando materiales y mano de obra
            var total = costo.costoTotal();
            // Calcula la diferencia entre lo presupuestado y el costo ejecutado.
            return new RentabilidadDTO(obraId, presupuesto, costo.getMateriales(),
                    costo.costoManoObra(), total, presupuesto.subtract(total));
        });
    }

    /** El Técnico solo opera sobre las obras que tiene asignadas (HU-24). */
    private Obra buscarObra(Long obraId, Usuario actor) {
        // Busca la obra solicitada.
        var obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Obra no encontrada"));
        // Un técnico solo puede operar sobre sus propias obras.
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
