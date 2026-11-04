package com.inacap.fyagobras.domain.presupuesto;

import com.inacap.fyagobras.domain.cliente.Cliente;
import com.inacap.fyagobras.domain.presupuesto.dto.ItemPresupuestoDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "presupuestos")
@Getter
@NoArgsConstructor
public class Presupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_presupuesto", nullable = false, unique = true, updatable = false)
    private String numeroPresupuesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPresupuesto estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    @Column(name = "fecha_aprobacion")
    private LocalDate fechaAprobacion;

    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    @Column(name = "monto_total", nullable = false)
    private BigDecimal montoTotal;

    @OneToMany(mappedBy = "presupuesto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPresupuesto> items = new ArrayList<>();

    // Todo presupuesto nuevo nace PENDIENTE
    public Presupuesto(String numeroPresupuesto, Cliente cliente, List<ItemPresupuestoDTO> items) {
        this.numeroPresupuesto = numeroPresupuesto;
        this.cliente = cliente;
        this.estado = EstadoPresupuesto.PENDIENTE;
        this.fechaCreacion = LocalDate.now();
        reemplazarItems(items);
    }

    public boolean estaPendiente() {
        return estado == EstadoPresupuesto.PENDIENTE;
    }

    // Cambia los ítems y recalcula el monto total (suma de los ítems)
    public void reemplazarItems(List<ItemPresupuestoDTO> nuevos) {
        items.clear();
        nuevos.forEach(i -> items.add(new ItemPresupuesto(this, i.descripcion(), i.monto())));
        montoTotal = items.stream()
                .map(ItemPresupuesto::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}