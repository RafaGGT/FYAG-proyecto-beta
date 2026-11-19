package com.inacap.fyagobras.domain.incidencia;

import com.inacap.fyagobras.domain.obra.Obra;
import com.inacap.fyagobras.domain.user.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Novedad registrada sobre una obra. Sin setters: una vez guardada no se modifica. */
@Entity
@Table(name = "incidencias_obra")
@Getter
@NoArgsConstructor
public class IncidenciaObra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obra_id")
    private Obra obra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoIncidencia tipo;

    @Column(nullable = false, length = 500)
    private String texto;

    @Column(nullable = false)
    private Instant fecha = Instant.now();

    public IncidenciaObra(Obra obra, Usuario autor, TipoIncidencia tipo, String texto) {
        this.obra = obra;
        this.autor = autor;
        this.tipo = tipo;
        this.texto = texto;
    }
}