package com.inacap.fyagobras.costo;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.cliente.Cliente;
import com.inacap.fyagobras.domain.costo.CostoReal;
import com.inacap.fyagobras.domain.costo.CostoRealService;
import com.inacap.fyagobras.domain.costo.repository.CostoRealRepository;
import com.inacap.fyagobras.domain.obra.Obra;
import com.inacap.fyagobras.domain.obra.repository.ObraRepository;
import com.inacap.fyagobras.domain.presupuesto.Presupuesto;
import com.inacap.fyagobras.domain.presupuesto.dto.ItemPresupuestoDTO;
import com.inacap.fyagobras.domain.servicio.TipoServicio;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * CP: Cálculo de rentabilidad de una obra (presupuesto aprobado versus costo real).
 * Trazabilidad: HU-04. Tipo: funcional. Herramienta: JUnit 5 (automatizada).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Cálculo de rentabilidad de la obra (HU-04)")
// Prueba la diferencia entre lo presupuestado y el costo real ejecutado.
class RentabilidadTest {

    private static final Long OBRA_ID = 100L;

    // Repositorios simulados para consultar la obra y sus costos reales.
    @Mock private CostoRealRepository costoRepository;
    @Mock private ObraRepository obraRepository;
    @Mock private AuditoriaService auditoriaService;
    // Servicio real al que se conectan los mocks anteriores.
    @InjectMocks private CostoRealService service;

    private static Obra obraConPresupuesto(BigDecimal montoPresupuesto) {
        // Construye el cliente asociado al presupuesto.
        var cliente = new Cliente();
        cliente.setId(1L);
        cliente.setRazonSocial("Constructora Andes SpA");

        // Crea y aprueba el presupuesto con el monto indicado.
        var presupuesto = new Presupuesto("PRES-000001", cliente,
                List.of(new ItemPresupuestoDTO("Instalación", montoPresupuesto)));
        ReflectionTestUtils.setField(presupuesto, "id", 10L);
        presupuesto.aprobar();

        // Crea el tipo de servicio utilizado por la obra.
        var tipo = new TipoServicio("Instalación eléctrica");
        ReflectionTestUtils.setField(tipo, "id", 3L);

        // Construye la obra sin técnico asignado (no es relevante para la rentabilidad).
        var obra = new Obra("Instalación bodega", "Obra de prueba",
                LocalDate.of(2026, 10, 1), presupuesto, tipo, null);
        ReflectionTestUtils.setField(obra, "id", OBRA_ID);
        return obra;
    }

    @Test
    @DisplayName("Calcula la diferencia entre el presupuesto aprobado y el costo real ejecutado")
    void calculaDiferenciaEntrePresupuestoYCostoReal() {
        // Simula una obra con presupuesto aprobado por 500.000.
        var obra = obraConPresupuesto(new BigDecimal("500000"));
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra));

        // Simula los costos reales: 100.000 en materiales y 10 horas a 5.000 cada una.
        var costo = new CostoReal(obra, new BigDecimal("100000"), new BigDecimal("10"), new BigDecimal("5000"));
        when(costoRepository.findByObraId(OBRA_ID)).thenReturn(Optional.of(costo));

        // Ejecuta el cálculo de rentabilidad.
        var resultado = service.rentabilidad(OBRA_ID);

        // Verifica que el resultado exista y que los montos sean los esperados.
        assertThat(resultado).isPresent();
        assertThat(resultado.get().presupuestoAprobado()).isEqualByComparingTo("500000");
        assertThat(resultado.get().costoMateriales()).isEqualByComparingTo("100000");
        // 10 horas * 5.000 = 50.000 de mano de obra.
        assertThat(resultado.get().costoManoObra()).isEqualByComparingTo("50000");
        // Costo total = materiales + mano de obra = 150.000.
        assertThat(resultado.get().costoTotal()).isEqualByComparingTo("150000");
        // Diferencia = presupuesto - costo total = 500.000 - 150.000 = 350.000.
        assertThat(resultado.get().diferencia()).isEqualByComparingTo("350000");
    }

    @Test
    @DisplayName("Sin costos reales registrados, la rentabilidad no está disponible")
    void sinCostosRegistradosNoHayRentabilidad() {
        // Simula una obra existente pero sin costos reales cargados todavía.
        var obra = obraConPresupuesto(new BigDecimal("500000"));
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra));
        when(costoRepository.findByObraId(OBRA_ID)).thenReturn(Optional.empty());

        // Ejecuta el cálculo y verifica que no haya resultado.
        var resultado = service.rentabilidad(OBRA_ID);

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Si la obra no existe, se lanza RecursoNoEncontradoException")
    void obraInexistenteLanzaExcepcion() {
        // Simula que no existe ninguna obra con el ID solicitado.
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.empty());

        // Ejecuta el cálculo y verifica el tipo y mensaje de la excepción.
        assertThatThrownBy(() -> service.rentabilidad(OBRA_ID))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Obra no encontrada");
    }
}