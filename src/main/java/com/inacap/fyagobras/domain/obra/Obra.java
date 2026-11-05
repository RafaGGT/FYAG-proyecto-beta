package com.inacap.fyagobras.domain.obra;

import com.inacap.fyagobras.domain.presupuesto.Presupuesto;
import com.inacap.fyagobras.domain.servicio.TipoServicio;
import com.inacap.fyagobras.domain.user.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "obras")
@Getter
@NoArgsConstructor
public class Obra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoObra estado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_termino")
    private LocalDate fechaTermino;

    @Column(name = "motivo_suspension", length = 500)
    private String motivoSuspension;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presupuesto_id")
    private Presupuesto presupuesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_servicio_id")
    private TipoServicio tipoServicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico;

    public Obra(String nombre, String descripcion, LocalDate fechaInicio,
                Presupuesto presupuesto, TipoServicio tipoServicio, Usuario tecnico) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.presupuesto = presupuesto;
        this.tipoServicio = tipoServicio;
        this.tecnico = tecnico;
        this.estado = EstadoObra.EN_PROCESO;
    }

    public void cambiarEstado(EstadoObra destino, String motivo) {
        if (destino == EstadoObra.SUSPENDIDA) {
            this.motivoSuspension = motivo.trim();
        }
        this.estado = destino;
    }
}