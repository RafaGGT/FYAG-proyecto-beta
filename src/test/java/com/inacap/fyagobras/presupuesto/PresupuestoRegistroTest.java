package com.inacap.fyagobras.presupuesto;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.cliente.Cliente;
import com.inacap.fyagobras.domain.cliente.repository.ClienteRepository;
import com.inacap.fyagobras.domain.presupuesto.EstadoPresupuesto;
import com.inacap.fyagobras.domain.presupuesto.Presupuesto;
import com.inacap.fyagobras.domain.presupuesto.PresupuestoService;
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
// Prueba el registro y validacion de presupuestos.
class PresupuestoRegistroTest {

    // Validador utilizado para comprobar las restricciones declaradas en el DTO.
    private static Validator validator;

    // Simula el repositorio de presupuestos para evitar usar la base de datos real.
    @Mock private PresupuestoRepository presupuestoRepository;
    // Simula la busqueda de clientes asociados al presupuesto.
    @Mock private ClienteRepository clienteRepository;
    // Simula el registro de auditoria para comprobar que no intervenga en la prueba.
    @Mock private AuditoriaService auditoriaService;
    // Inserta los mocks anteriores dentro del servicio que se probara.
    @InjectMocks private PresupuestoService service;

    @BeforeAll
    static void iniciarValidador() {
        // Construye una fabrica temporal de validacion para ejecutar las restricciones Jakarta.
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static Cliente cliente() {
        // Crea el cliente minimo necesario para completar el registro.
        var c = new Cliente();
        c.setId(1L);
        c.setRazonSocial("Constructora Andes SpA");
        return c;
    }

    private static List<ItemPresupuestoDTO> items() {
        // Devuelve los items que integraran el monto total del presupuesto.
        return List.of(
                new ItemPresupuestoDTO("Instalación eléctrica", new BigDecimal("120000")),
                new ItemPresupuestoDTO("Materiales", new BigDecimal("30000")));
    }

    private static List<String> mensajes(RegistrarPresupuestoDTO dto) {
        // Ejecuta todas las validaciones del DTO y extrae sus mensajes de error.
        return validator.validate(dto).stream().map(ConstraintViolation::getMessage).toList();
    }

    @Test
    @DisplayName("Con cliente e ítems queda en estado PENDIENTE y el monto es la suma de los ítems")
    void conDatosCompletosQuedaPendiente() {
        // Simula que el cliente con ID 1 existe en la base de datos.
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente()));
        // Simula que sera el primer presupuesto generado por el sistema.
        when(presupuestoRepository.siguienteNumero()).thenReturn(1L);
        // Devuelve el mismo presupuesto recibido al intentar guardarlo.
        when(presupuestoRepository.save(any(Presupuesto.class))).thenAnswer(inv -> inv.getArgument(0));

        // Construye un DTO valido con cliente e items.
        var datos = new RegistrarPresupuestoDTO(1L, items());

        // Comprueba que el DTO completo no produzca errores de validacion.
        assertThat(mensajes(datos)).isEmpty();

        // Ejecuta la operacion real del servicio de presupuestos.
        var resultado = service.registrar(datos);

        // Verifica el estado inicial asignado al presupuesto.
        assertThat(resultado.estado()).isEqualTo(EstadoPresupuesto.PENDIENTE);
        // Comprueba la numeracion generada a partir del siguiente numero disponible.
        assertThat(resultado.numeroPresupuesto()).isEqualTo("PRES-000001");
        // Comprueba que la fecha de creacion corresponda a la fecha actual.
        assertThat(resultado.fechaCreacion()).isEqualTo(LocalDate.now());
        // Comprueba que todavia no exista una fecha de aprobacion.
        assertThat(resultado.fechaAprobacion()).isNull();
        // Comprueba que el presupuesto conserve los dos items registrados.
        assertThat(resultado.items()).hasSize(2);
        // Verifica que el monto total sea la suma de 120000 y 30000.
        assertThat(resultado.montoTotal()).isEqualByComparingTo("150000");
        // Confirma que el repositorio efectivamente intento guardar el presupuesto.
        verify(presupuestoRepository).save(any(Presupuesto.class));
    }

    @Test
    @DisplayName("Sin cliente (clienteId nulo) el DTO se rechaza")
    void sinClienteSeRechaza() {
        // Crea un presupuesto sin identificar al cliente.
        var datos = new RegistrarPresupuestoDTO(null, items());

        // Comprueba el mensaje exacto emitido por la validacion del DTO.
        assertThat(mensajes(datos)).containsExactly("El cliente es obligatorio");
    }

    @Test
    @DisplayName("Con un cliente que no existe el servicio lanza RecursoNoEncontradoException y no guarda")
    void clienteInexistenteSeRechaza() {
        // Simula que no existe ningun cliente con el ID 99.
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        // Construye datos que apuntan al cliente inexistente.
        var datos = new RegistrarPresupuestoDTO(99L, items());

        // Ejecuta el registro y verifica el tipo y mensaje de la excepcion.
        assertThatThrownBy(() -> service.registrar(datos))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Cliente no encontrado");
        // Comprueba que nunca se intenta persistir el presupuesto invalido.
        verify(presupuestoRepository, never()).save(any(Presupuesto.class));
    }

    @Test
    @DisplayName("Sin ítems (lista vacía) el DTO se rechaza")
    void sinItemsListaVaciaSeRechaza() {
        // Crea un presupuesto con una lista de items vacia.
        var datos = new RegistrarPresupuestoDTO(1L, List.of());

        // Comprueba que la validacion rechace la lista vacia.
        assertThat(mensajes(datos)).containsExactly("Debe incluir al menos un ítem");
    }

    @Test
    @DisplayName("Sin ítems (lista nula) el DTO se rechaza")
    void sinItemsListaNulaSeRechaza() {
        // Crea un presupuesto sin proporcionar la lista de items.
        var datos = new RegistrarPresupuestoDTO(1L, null);

        // Comprueba que la validacion trate el valor nulo como invalido.
        assertThat(mensajes(datos)).containsExactly("Debe incluir al menos un ítem");
    }

    @Test
    @DisplayName("Un ítem con monto cero se rechaza")
    void itemConMontoCeroSeRechaza() {
        // Construye un item cuyo monto no supera el limite permitido.
        var datos = new RegistrarPresupuestoDTO(1L,
                List.of(new ItemPresupuestoDTO("Mano de obra", BigDecimal.ZERO)));

        // Comprueba que el monto cero genere el mensaje de error correspondiente.
        assertThat(mensajes(datos)).containsExactly("El monto debe ser mayor que cero");
    }
}
