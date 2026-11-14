package com.inacap.fyagobras.domain.presupuesto;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.cliente.Cliente;
import com.inacap.fyagobras.domain.cliente.repository.ClienteRepository;
import com.inacap.fyagobras.domain.presupuesto.dto.ItemPresupuestoDTO;
import com.inacap.fyagobras.domain.presupuesto.dto.RegistrarPresupuestoDTO;
import com.inacap.fyagobras.domain.presupuesto.repository.PresupuestoRepository;
import com.inacap.fyagobras.infra.exception.RecursoNoEncontradoException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CP: Registrar presupuesto nuevo con y sin cliente/ítems.
 * Trazabilidad: HU-01, RF-01. Tipo: funcional. Herramienta: JUnit 5 (automatizada).
 *
 * La regla "sin cliente / sin ítem" se aplica en dos capas:
 *  - Bean Validation sobre el DTO (cliente nulo, lista de ítems vacía), que el controller dispara con @Valid.
 *  - PresupuestoService, que rechaza un cliente que no existe en la base de datos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Registro de presupuestos (HU-01 / RF-01)")
class PresupuestoRegistroTest {

    private static Validator validator;

    @Mock private PresupuestoRepository presupuestoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private AuditoriaService auditoriaService;
    @InjectMocks private PresupuestoService service;

    @BeforeAll
    static void iniciarValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static Cliente cliente() {
        var c = new Cliente();
        c.setId(1L);
        c.setRazonSocial("Constructora Andes SpA");
        return c;
    }

    private static List<ItemPresupuestoDTO> items() {
        return List.of(
                new ItemPresupuestoDTO("Instalación eléctrica", new BigDecimal("120000")),
                new ItemPresupuestoDTO("Materiales", new BigDecimal("30000")));
    }

    private static List<String> mensajes(RegistrarPresupuestoDTO dto) {
        return validator.validate(dto).stream().map(ConstraintViolation::getMessage).toList();
    }

    @Test
    @DisplayName("Con cliente e ítems queda en estado PENDIENTE y el monto es la suma de los ítems")
    void conDatosCompletosQuedaPendiente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente()));
        when(presupuestoRepository.siguienteNumero()).thenReturn(1L);
        when(presupuestoRepository.save(any(Presupuesto.class))).thenAnswer(inv -> inv.getArgument(0));

        var datos = new RegistrarPresupuestoDTO(1L, items());

        // El DTO completo no debe tener errores de validación
        assertThat(mensajes(datos)).isEmpty();

        var resultado = service.registrar(datos);

        assertThat(resultado.estado()).isEqualTo(EstadoPresupuesto.PENDIENTE);
        assertThat(resultado.numeroPresupuesto()).isEqualTo("PRES-000001");
        assertThat(resultado.fechaCreacion()).isEqualTo(LocalDate.now());
        assertThat(resultado.fechaAprobacion()).isNull();
        assertThat(resultado.items()).hasSize(2);
        assertThat(resultado.montoTotal()).isEqualByComparingTo("150000");
        verify(presupuestoRepository).save(any(Presupuesto.class));
    }

    @Test
    @DisplayName("Sin cliente (clienteId nulo) el DTO se rechaza")
    void sinClienteSeRechaza() {
        var datos = new RegistrarPresupuestoDTO(null, items());

        assertThat(mensajes(datos)).containsExactly("El cliente es obligatorio");
    }

    @Test
    @DisplayName("Con un cliente que no existe el servicio lanza RecursoNoEncontradoException y no guarda")
    void clienteInexistenteSeRechaza() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        var datos = new RegistrarPresupuestoDTO(99L, items());

        assertThatThrownBy(() -> service.registrar(datos))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Cliente no encontrado");
        verify(presupuestoRepository, never()).save(any(Presupuesto.class));
    }

    @Test
    @DisplayName("Sin ítems (lista vacía) el DTO se rechaza")
    void sinItemsListaVaciaSeRechaza() {
        var datos = new RegistrarPresupuestoDTO(1L, List.of());

        assertThat(mensajes(datos)).containsExactly("Debe incluir al menos un ítem");
    }

    @Test
    @DisplayName("Sin ítems (lista nula) el DTO se rechaza")
    void sinItemsListaNulaSeRechaza() {
        var datos = new RegistrarPresupuestoDTO(1L, null);

        assertThat(mensajes(datos)).containsExactly("Debe incluir al menos un ítem");
    }

    @Test
    @DisplayName("Un ítem con monto cero se rechaza")
    void itemConMontoCeroSeRechaza() {
        var datos = new RegistrarPresupuestoDTO(1L,
                List.of(new ItemPresupuestoDTO("Mano de obra", BigDecimal.ZERO)));

        assertThat(mensajes(datos)).containsExactly("El monto debe ser mayor que cero");
    }
}
