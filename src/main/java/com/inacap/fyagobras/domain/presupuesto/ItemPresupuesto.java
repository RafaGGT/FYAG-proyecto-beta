package com.inacap.fyagobras.domain.presupuesto;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// Línea de detalle de un presupuesto. No existe fuera de él (composición).
@Entity
@Table(name = "items_presupuesto")
@Getter
@NoArgsConstructor
public class ItemPresupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presupuesto_id")
    private Presupuesto presupuesto;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private BigDecimal monto;

    public ItemPresupuesto(Presupuesto presupuesto, String descripcion, BigDecimal monto) {
        this.presupuesto = presupuesto;
        this.descripcion = descripcion;
        this.monto = monto;
    }
}