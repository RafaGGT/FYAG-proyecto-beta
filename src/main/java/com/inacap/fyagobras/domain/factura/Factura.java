package com.inacap.fyagobras.domain.factura;

import com.inacap.fyagobras.domain.obra.Obra;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "facturas")
@Getter
@NoArgsConstructor
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_factura", nullable = false, unique = true, length = 30)
    private String numeroFactura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obra_id")
    private Obra obra;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Column(nullable = false)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false)
    private EstadoPago estadoPago;

    @Column(nullable = false)
    private boolean anulada;

    @Column(name = "motivo_anulacion", length = 500)
    private String motivoAnulacion;

    @Column(name = "anulada_por")
    private Long anuladaPor;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    public Factura(String numeroFactura, Obra obra, LocalDate fechaEmision,
                   LocalDate fechaVencimiento, BigDecimal monto) {
        this.numeroFactura = numeroFactura;
        this.obra = obra;
        this.fechaEmision = fechaEmision;
        this.fechaVencimiento = fechaVencimiento;
        this.monto = monto;
        // HU-13: el estado de pago inicial siempre es Pendiente
        this.estadoPago = EstadoPago.PENDIENTE;
        this.anulada = false;
    }

    /** HU-14: pasa de PENDIENTE a PAGADO registrando la fecha de pago. */
    public void registrarPago(LocalDate fechaPago) {
        // Marca la factura como pagada y conserva la fecha informada.
        this.estadoPago = EstadoPago.PAGADO;
        this.fechaPago = fechaPago;
    }

    /** HU-29: anula el pago por error; la factura vuelve a PENDIENTE y queda registro de quién, cuándo y por qué. */
    public void anularPago(String motivo, Long usuarioId) {
        // Devuelve la factura a pendiente sin eliminar el registro original.
        this.estadoPago = EstadoPago.PENDIENTE;
        this.fechaPago = null;
        // Conserva quién, cuándo y por qué se anuló el pago.
        this.motivoAnulacion = motivo;
        this.anuladaPor = usuarioId;
        this.fechaAnulacion = LocalDateTime.now();
    }
}