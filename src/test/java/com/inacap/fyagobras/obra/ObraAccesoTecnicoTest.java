package com.inacap.fyagobras.obra;

import com.inacap.fyagobras.domain.audit.AuditoriaService;
import com.inacap.fyagobras.domain.cliente.Cliente;
import com.inacap.fyagobras.domain.costo.repository.CostoRealRepository;
import com.inacap.fyagobras.domain.factura.repository.FacturaRepository;
import com.inacap.fyagobras.domain.notificacion.NotificacionService;
import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.obra.Obra;
import com.inacap.fyagobras.domain.obra.ObraService;
import com.inacap.fyagobras.domain.obra.dto.AsignarTecnicoDTO;
import com.inacap.fyagobras.domain.obra.dto.RegistrarObraDTO;
import com.inacap.fyagobras.domain.obra.repository.ObraRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CP: El Técnico solo ve y consulta sus obras; solo se asigna un técnico activo; el tipo de servicio
 * debe estar vigente.
 * Trazabilidad: HU-24, HU-25, HU-07, HU-08, OWASP A01. Tipo: funcional y seguridad (unitaria).
 * Herramienta: JUnit 5 (automatizada).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Acceso del Técnico y asignación de responsable (HU-24 / HU-25 / HU-07 / HU-08)")
// Prueba que el filtro por técnico y las reglas de asignación se apliquen en el servicio (backend).
class ObraAccesoTecnicoTest {

    private static final Long OBRA_ID = 100L;
    private static final Long TECNICO_1 = 5L;
    private static final Long TECNICO_2 = 6L;
    private static final Pageable PAGINA = PageRequest.of(0, 10);

    // Repositorios y colaboradores simulados que conectan al servicio real.
    @Mock private ObraRepository obraRepository;
    @Mock private PresupuestoRepository presupuestoRepository;
    @Mock private TipoServicioRepository tipoServicioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private CostoRealRepository costoRepository;
    @Mock private FacturaRepository facturaRepository;
    @Mock private NotificacionService notificacionService;
    // Servicio real al que se conectan los mocks anteriores.
    @InjectMocks private ObraService service;

    private Usuario supervisor;
    private Usuario tecnico1;

    @BeforeEach
    void preparar() {
        // Supervisor que ejecuta las asignaciones.
        supervisor = usuario(1L, Role.SUPERVISOR, true);
        // Técnico 1: el que inicia sesión en los escenarios de consulta.
        tecnico1 = usuario(TECNICO_1, Role.TECNICO, true);
    }

    private static Usuario usuario(Long id, Role rol, boolean activo) {
        // Construye un usuario con el rol y el estado indicados.
        var u = new Usuario();
        u.setId(id);
        u.setNombre("Usuario" + id);
        u.setApellido("Prueba");
        u.setRole(rol);
        u.setActivo(activo);
        return u;
    }

    private static Obra obra(EstadoObra estado, Usuario tecnico) {
        // Construye una obra a partir de un presupuesto aprobado y la lleva al estado pedido.
        var cliente = new Cliente();
        cliente.setId(1L);
        cliente.setRazonSocial("Constructora Andes SpA");
        var presupuesto = new Presupuesto("PRES-000001", cliente,
                List.of(new ItemPresupuestoDTO("Instalación", new BigDecimal("200000"))));
        ReflectionTestUtils.setField(presupuesto, "id", 10L);
        presupuesto.aprobar();
        var tipo = new TipoServicio("Instalación eléctrica");
        ReflectionTestUtils.setField(tipo, "id", 3L);
        var obra = new Obra("Instalación bodega", "Obra de prueba",
                LocalDate.of(2026, 10, 1), presupuesto, tipo, tecnico);
        ReflectionTestUtils.setField(obra, "id", OBRA_ID);
        if (estado != EstadoObra.EN_PROCESO) {
            obra.cambiarEstado(estado, "motivo de prueba");
        }
        return obra;
    }

    // ---------- HU-24: el Técnico solo ve sus obras ----------

    @Test
    @DisplayName("El Técnico lista solo sus obras: el servicio consulta por técnico y nunca todas")
    void tecnicoListaSoloSusObras() {
        // Simula que el repositorio entrega únicamente las obras del técnico 1.
        when(obraRepository.findByTecnicoId(TECNICO_1, PAGINA))
                .thenReturn(new PageImpl<>(List.of(obra(EstadoObra.EN_PROCESO, tecnico1))));

        var resultado = service.listar(null, tecnico1, PAGINA);

        // Verifica que llegue su obra y que no se haya consultado el listado general.
        assertThat(resultado.getContent()).hasSize(1);
        verify(obraRepository, never()).findAll(any(Pageable.class));
        verify(obraRepository, never()).findByEstado(any(), any());
    }

    @Test
    @DisplayName("El Técnico filtrando por estado sigue viendo solo sus obras")
    void tecnicoConFiltroDeEstadoSoloVeSusObras() {
        when(obraRepository.findByTecnicoIdAndEstado(TECNICO_1, EstadoObra.EN_PROCESO, PAGINA))
                .thenReturn(new PageImpl<>(List.of(obra(EstadoObra.EN_PROCESO, tecnico1))));

        var resultado = service.listar(EstadoObra.EN_PROCESO, tecnico1, PAGINA);

        assertThat(resultado.getContent()).hasSize(1);
        // El filtro por estado nunca debe saltarse el filtro por técnico.
        verify(obraRepository, never()).findByEstado(any(), any());
    }

    @Test
    @DisplayName("El Supervisor lista todas las obras")
    void supervisorListaTodas() {
        when(obraRepository.findAll(PAGINA))
                .thenReturn(new PageImpl<>(List.of(obra(EstadoObra.EN_PROCESO, tecnico1))));

        var resultado = service.listar(null, supervisor, PAGINA);

        assertThat(resultado.getContent()).hasSize(1);
        verify(obraRepository, never()).findByTecnicoId(any(), any());
    }

    @Test
    @DisplayName("El Técnico puede consultar por id una obra que tiene asignada")
    void tecnicoConsultaSuObra() {
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra(EstadoObra.EN_PROCESO, tecnico1)));

        var resultado = service.obtener(OBRA_ID, tecnico1);

        assertThat(resultado.id()).isEqualTo(OBRA_ID);
    }

    @Test
    @DisplayName("El Técnico no puede consultar por id la obra de otro técnico (acceso denegado)")
    void tecnicoNoConsultaObraAjena() {
        // La obra pertenece al técnico 2, pero la consulta la hace el técnico 1.
        var ajena = obra(EstadoObra.EN_PROCESO, usuario(TECNICO_2, Role.TECNICO, true));
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> service.obtener(OBRA_ID, tecnico1))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Solo puede consultar las obras que tiene asignadas.");
    }

    @Test
    @DisplayName("El Técnico no puede consultar una obra que no tiene técnico asignado")
    void tecnicoNoConsultaObraSinTecnico() {
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra(EstadoObra.EN_PROCESO, null)));

        assertThatThrownBy(() -> service.obtener(OBRA_ID, tecnico1))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("El Supervisor puede consultar la obra de cualquier técnico")
    void supervisorConsultaCualquierObra() {
        var ajena = obra(EstadoObra.EN_PROCESO, usuario(TECNICO_2, Role.TECNICO, true));
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(ajena));

        assertThat(service.obtener(OBRA_ID, supervisor).id()).isEqualTo(OBRA_ID);
    }

    // ---------- HU-07: asignar técnico ----------

    @Test
    @DisplayName("Asignar un usuario que no es técnico se rechaza")
    void asignarUsuarioQueNoEsTecnicoSeRechaza() {
        var obra = obra(EstadoObra.EN_PROCESO, tecnico1);
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra));
        // El usuario elegido tiene rol FINANZAS.
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(usuario(9L, Role.FINANZAS, true)));

        assertThatThrownBy(() -> service.asignarTecnico(OBRA_ID, new AsignarTecnicoDTO(9L), supervisor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("Debe seleccionar un técnico activo.");

        // El responsable original no cambia y no se audita.
        assertThat(obra.getTecnico().getId()).isEqualTo(TECNICO_1);
        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Asignar un técnico desactivado se rechaza")
    void asignarTecnicoDesactivadoSeRechaza() {
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra(EstadoObra.EN_PROCESO, tecnico1)));
        when(usuarioRepository.findById(TECNICO_2))
                .thenReturn(Optional.of(usuario(TECNICO_2, Role.TECNICO, false)));

        assertThatThrownBy(() -> service.asignarTecnico(OBRA_ID, new AsignarTecnicoDTO(TECNICO_2), supervisor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("Debe seleccionar un técnico activo.");
    }

    @ParameterizedTest(name = "Obra {0}: no se puede cambiar el técnico")
    @EnumSource(value = EstadoObra.class, names = {"FINALIZADA", "CANCELADA"})
    @DisplayName("Una obra cerrada (FINALIZADA o CANCELADA) no admite cambiar al responsable")
    void obraCerradaNoAdmiteCambiarTecnico(EstadoObra estado) {
        var obra = obra(estado, tecnico1);
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra));

        assertThatThrownBy(() -> service.asignarTecnico(OBRA_ID, new AsignarTecnicoDTO(TECNICO_2), supervisor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("No se puede cambiar el técnico de una obra " + estado + ".");

        assertThat(obra.getTecnico().getId()).isEqualTo(TECNICO_1);
    }

    @Test
    @DisplayName("Reemplazar al técnico por otro activo queda auditado con el anterior y el nuevo")
    void reemplazarTecnicoQuedaAuditado() {
        var obra = obra(EstadoObra.EN_PROCESO, tecnico1);
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra));
        var nuevo = usuario(TECNICO_2, Role.TECNICO, true);
        when(usuarioRepository.findById(TECNICO_2)).thenReturn(Optional.of(nuevo));

        var resultado = service.asignarTecnico(OBRA_ID, new AsignarTecnicoDTO(TECNICO_2), supervisor);

        // La obra queda con el nuevo responsable.
        assertThat(resultado.tecnicoId()).isEqualTo(TECNICO_2);
        // Se registra quién lo hizo, de quién a quién.
        verify(auditoriaService).registrar(eq(supervisor), eq("obras"), eq(OBRA_ID), eq("ASIGNAR_TECNICO"),
                isNull(),
                eq(Map.<String, Object>of("tecnico_id", TECNICO_1)),
                eq(Map.<String, Object>of("tecnico_id", TECNICO_2)));
    }

    @Test
    @DisplayName("Asignar al mismo técnico no cambia nada ni genera auditoría")
    void asignarAlMismoTecnicoNoAudita() {
        when(obraRepository.findById(OBRA_ID)).thenReturn(Optional.of(obra(EstadoObra.EN_PROCESO, tecnico1)));
        when(usuarioRepository.findById(TECNICO_1)).thenReturn(Optional.of(tecnico1));

        service.asignarTecnico(OBRA_ID, new AsignarTecnicoDTO(TECNICO_1), supervisor);

        verify(auditoriaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    // ---------- HU-08 y HU-07 al crear la obra ----------

    @Test
    @DisplayName("No se crea una obra con un tipo de servicio desactivado")
    void tipoDeServicioDesactivadoNoSeUsaEnObraNueva() {
        var presupuesto = obra(EstadoObra.EN_PROCESO, null).getPresupuesto();
        when(presupuestoRepository.findById(10L)).thenReturn(Optional.of(presupuesto));
        when(obraRepository.existsByPresupuestoId(10L)).thenReturn(false);
        // El tipo existe, pero fue desactivado por el Supervisor (HU-08).
        var tipoInactivo = new TipoServicio("Garantía");
        tipoInactivo.cambiarEstado(false);
        when(tipoServicioRepository.findById(3L)).thenReturn(Optional.of(tipoInactivo));

        var datos = new RegistrarObraDTO("Obra nueva", "Prueba", 10L, 3L, TECNICO_1, LocalDate.of(2026, 11, 20));

        assertThatThrownBy(() -> service.registrar(datos, supervisor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("El tipo de servicio ya no está vigente.");
        verify(obraRepository, never()).save(any(Obra.class));
    }

    @Test
    @DisplayName("No se crea una obra con un técnico desactivado")
    void tecnicoDesactivadoNoSeUsaEnObraNueva() {
        var presupuesto = obra(EstadoObra.EN_PROCESO, null).getPresupuesto();
        when(presupuestoRepository.findById(10L)).thenReturn(Optional.of(presupuesto));
        when(obraRepository.existsByPresupuestoId(10L)).thenReturn(false);
        when(tipoServicioRepository.findById(3L)).thenReturn(Optional.of(new TipoServicio("Mantención")));
        when(usuarioRepository.findById(TECNICO_2))
                .thenReturn(Optional.of(usuario(TECNICO_2, Role.TECNICO, false)));

        var datos = new RegistrarObraDTO("Obra nueva", "Prueba", 10L, 3L, TECNICO_2, LocalDate.of(2026, 11, 20));

        assertThatThrownBy(() -> service.registrar(datos, supervisor))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("Debe seleccionar un técnico activo.");
        verify(obraRepository, never()).save(any(Obra.class));
    }
}