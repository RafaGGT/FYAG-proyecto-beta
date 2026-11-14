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
import com.inacap.fyagobras.infra.exception.ConflictoNegocioException;

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
        // Indica si el presupuesto todavía puede ser editado o resuelto.
        return estado == EstadoPresupuesto.PENDIENTE;
    }

    // Cambia los ítems y recalcula el monto total (suma de los ítems)
    public void reemplazarItems(List<ItemPresupuestoDTO> nuevos) {
        // Elimina los ítems anteriores para reemplazarlos por la nueva versión.
        items.clear();
        // Crea cada ítem vinculándolo con este presupuesto.
        nuevos.forEach(i -> items.add(new ItemPresupuesto(this, i.descripcion(), i.monto())));
        // Recalcula el total a partir de los montos actuales.
        montoTotal = items.stream()
                .map(ItemPresupuesto::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** HU-02: solo se aprueba con cliente, montos y al menos un ítem. */
    public void aprobar() {
        // Comprueba que el presupuesto tenga datos suficientes para aprobarse.
        boolean sinItems = items.isEmpty();
        boolean montoInvalido = items.stream()
                .anyMatch(i -> i.getMonto() == null || i.getMonto().signum() <= 0);
        if (cliente == null || sinItems || montoInvalido) {
            throw new ConflictoNegocioException(
                    "Para aprobar, el presupuesto debe tener cliente y al menos un ítem con monto.");
        }
        // Cambia el estado y registra la fecha de aprobación.
        this.estado = EstadoPresupuesto.APROBADO;
        this.fechaAprobacion = LocalDate.now();
        this.motivoRechazo = null;
    }

    public void rechazar(String motivo) {
        // Marca el presupuesto como rechazado y conserva el motivo.
        this.estado = EstadoPresupuesto.RECHAZADO;
        this.motivoRechazo = motivo;
    }
}