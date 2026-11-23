package com.inacap.fyagobras.obra;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.cliente.Cliente;
import com.inacap.fyagobras.domain.costo.repository.CostoRealRepository;
import com.inacap.fyagobras.domain.factura.repository.FacturaRepository;
import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.obra.Obra;
import com.inacap.fyagobras.domain.obra.ObraService;
import com.inacap.fyagobras.domain.obra.dto.RegistrarObraDTO;
import com.inacap.fyagobras.domain.obra.repository.ObraRepository;
import com.inacap.fyagobras.domain.presupuesto.EstadoPresupuesto;
import com.inacap.fyagobras.domain.presupuesto.Presupuesto;
import com.inacap.fyagobras.domain.presupuesto.dto.ItemPresupuestoDTO;
import com.inacap.fyagobras.domain.presupuesto.repository.PresupuestoRepository;
import com.inacap.fyagobras.domain.servicio.TipoServicio;
import com.inacap.fyagobras.domain.servicio.repository.TipoServicioRepository;
import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import com.inacap.fyagobras.domain.user.repository.UsuarioRepository;
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.inacap.fyagobras.domain.notificacion.NotificacionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CP: Crear obra con presupuesto aprobado y no aprobado.
 * Trazabilidad: HU-05, RF-02. Tipo: funcional. Herramienta: JUnit 5 (automatizada).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Creación de obras desde presupuesto (HU-05 / RF-02)")
// Prueba los casos de creacion y validacion de obras.
class ObraCreacionTest {

    // Identificadores fijos utilizados por las búsquedas simuladas.
    private static final Long PRESUPUESTO_ID = 10L;
    private static final Long TIPO_ID = 3L;
    private static final Long TECNICO_ID = 5L;

    // Repositorios simulados necesarios para construir y persistir una obra.
    @Mock private ObraRepository obraRepository;
    @Mock private PresupuestoRepository presupuestoRepository;
    @Mock private TipoServicioRepository tipoServicioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private CostoRealRepository costoRepository;
    @Mock private FacturaRepository facturaRepository;
    @Mock private NotificacionService notificacionService;
    // Servicio real que será probado con los mocks configurados.
    @InjectMocks private ObraService service;

    // Usuario que ejecutará la acción y aparecerá en la auditoría.
    private Usuario actor;
    // Datos comunes usados para solicitar la creación de la obra.
    private RegistrarObraDTO datos;

    @BeforeEach
    void preparar() {
        // Crea el usuario supervisor que realizará la operación.
        actor = new Usuario();
        actor.setId(1L);
        actor.setRole(Role.SUPERVISOR);

        // Construye los datos de entrada de la obra.
        datos = new RegistrarObraDTO("Instalación bodega", "Obra de prueba",
                PRESUPUESTO_ID, TIPO_ID, TECNICO_ID, LocalDate.of(2026, 11, 20));
    }

    private static Presupuesto presupuestoEn(EstadoPresupuesto estado) {
        // Construye el cliente requerido por el presupuesto.
        var cliente = new Cliente();
        cliente.setId(1L);
        cliente.setRazonSocial("Constructora Andes SpA");

        // Crea el presupuesto y asigna su identificador interno.
        var p = new Presupuesto("PRES-000001", cliente,
                List.of(new ItemPresupuestoDTO("Instalación", new BigDecimal("200000"))));
        ReflectionTestUtils.setField(p, "id", PRESUPUESTO_ID);
        // Aplica el estado indicado para probar los escenarios válidos e inválidos.
        if (estado == EstadoPresupuesto.APROBADO) {
            p.aprobar();
        } else if (estado == EstadoPresupuesto.RECHAZADO) {
            p.rechazar("Monto fuera de rango");
        }
        return p;
    }

    private static TipoServicio tipoVigente() {
        // Crea un tipo de servicio vigente con un identificador fijo.
        var tipo = new TipoServicio("Instalación eléctrica");
        ReflectionTestUtils.setField(tipo, "id", TIPO_ID);
        return tipo;
    }

    private static Usuario tecnicoActivo() {
        // Crea un técnico activo disponible para asignar a la obra.
        var t = new Usuario();
        t.setId(TECNICO_ID);
        t.setNombre("Juan");
        t.setApellido("Pérez");
        t.setRole(Role.TECNICO);
        return t;
    }

    @Test
    @DisplayName("Con presupuesto APROBADO la obra se crea en estado EN_PROCESO")
    void conPresupuestoAprobadoSeCreaLaObra() {
        // Simula la existencia del presupuesto aprobado.
        when(presupuestoRepository.findById(PRESUPUESTO_ID))
                .thenReturn(Optional.of(presupuestoEn(EstadoPresupuesto.APROBADO)));
        // Indica que el presupuesto todavía no tiene una obra asociada.
        when(obraRepository.existsByPresupuestoId(PRESUPUESTO_ID)).thenReturn(false);
        // Simula la existencia del tipo de servicio y del técnico.
        when(tipoServicioRepository.findById(TIPO_ID)).thenReturn(Optional.of(tipoVigente()));
        when(usuarioRepository.findById(TECNICO_ID)).thenReturn(Optional.of(tecnicoActivo()));
        // Asigna el identificador de obra cuando el repositorio persista la entidad.
        when(obraRepository.save(any(Obra.class))).thenAnswer(inv -> {
            Obra o = inv.getArgument(0);
            ReflectionTestUtils.setField(o, "id", 100L);
            return o;
        });

        // Ejecuta el registro de la obra.
        var resultado = service.registrar(datos, actor);

        // Verifica el identificador, estado y relaciones generadas.
        assertThat(resultado.id()).isEqualTo(100L);
        assertThat(resultado.estado()).isEqualTo(EstadoObra.EN_PROCESO);
        assertThat(resultado.presupuestoId()).isEqualTo(PRESUPUESTO_ID);
        assertThat(resultado.tecnicoId()).isEqualTo(TECNICO_ID);
        // Comprueba que se haya persistido la obra.
        verify(obraRepository).save(any(Obra.class));
        // Comprueba que se registre la creación en auditoría.
        verify(auditoriaService).registrar(eq(actor), eq("obras"), eq(100L), eq("CREAR_OBRA"),
                isNull(), isNull(), anyMap());
    }

    @ParameterizedTest(name = "Presupuesto {0}: no se crea la obra")
    @EnumSource(value = EstadoPresupuesto.class, names = {"PENDIENTE", "RECHAZADO"})
    @DisplayName("Con presupuesto no aprobado la obra se rechaza")
    void conPresupuestoNoAprobadoSeRechaza(EstadoPresupuesto estado) {
        // Simula que el presupuesto tiene un estado pendiente o rechazado.
        when(presupuestoRepository.findById(PRESUPUESTO_ID))
                .thenReturn(Optional.of(presupuestoEn(estado)));

        // Ejecuta el registro y verifica la excepción de negocio.
        assertThatThrownBy(() -> service.registrar(datos, actor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("Solo se puede crear una obra desde un presupuesto APROBADO.");

        // Confirma que no se persista ni se audite la creación rechazada.
        verify(obraRepository, never()).save(any(Obra.class));
        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Un presupuesto aprobado que ya tiene obra no admite una segunda obra")
    void presupuestoConObraNoAdmiteOtra() {
        // Simula un presupuesto aprobado que ya fue utilizado.
        when(presupuestoRepository.findById(PRESUPUESTO_ID))
                .thenReturn(Optional.of(presupuestoEn(EstadoPresupuesto.APROBADO)));
        when(obraRepository.existsByPresupuestoId(PRESUPUESTO_ID)).thenReturn(true);

        // Verifica que el servicio rechace la segunda obra.
        assertThatThrownBy(() -> service.registrar(datos, actor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("Ese presupuesto ya tiene una obra asociada.");

        // Comprueba que no se realice la persistencia adicional.
        verify(obraRepository, never()).save(any(Obra.class));
    }
}
