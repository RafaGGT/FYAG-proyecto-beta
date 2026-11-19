package com.inacap.fyagobras.domain.notificacion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Aviso para un usuario. Se guarda en el servidor para que lo vea desde cualquier dispositivo. */
@Entity
@Table(name = "notificaciones")
@Getter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "destinatario_id", nullable = false)
    private Long destinatarioId;

    @Column(name = "obra_id")
    private Long obraId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacion tipo;

    @Column(nullable = false, length = 300)
    private String mensaje;

    @Column(nullable = false, length = 10)
    private String tono;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion = Instant.now();

    public Notificacion(Long destinatarioId, Long obraId, TipoNotificacion tipo, String mensaje, String tono) {
        this.destinatarioId = destinatarioId;
        this.obraId = obraId;
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.tono = tono;
    }

    public void marcarLeida() {
        this.leida = true;
    }
}