package com.inacap.fyagobras.domain.costo;

import com.inacap.fyagobras.domain.obra.Obra;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "costos_reales")
@Getter
@NoArgsConstructor
public class CostoReal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obra_id")
    private Obra obra;

    @Column(nullable = false)
    private BigDecimal materiales;

    @Column(name = "horas_tecnicas", nullable = false)
    private BigDecimal horasTecnicas;

    @Column(name = "valor_hora", nullable = false)
    private BigDecimal valorHora;

    public CostoReal(Obra obra, BigDecimal materiales, BigDecimal horasTecnicas, BigDecimal valorHora) {
        this.obra = obra;
        actualizar(materiales, horasTecnicas, valorHora);
    }

    public void actualizar(BigDecimal materiales, BigDecimal horasTecnicas, BigDecimal valorHora) {
        this.materiales = materiales;
        this.horasTecnicas = horasTecnicas;
        this.valorHora = valorHora;
    }

    public BigDecimal costoManoObra() {
        return horasTecnicas.multiply(valorHora).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal costoTotal() {
        return materiales.add(costoManoObra());
    }
}