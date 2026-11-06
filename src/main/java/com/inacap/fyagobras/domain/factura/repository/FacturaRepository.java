package com.inacap.fyagobras.domain.factura.repository;

import com.inacap.fyagobras.domain.factura.EstadoPago;
import com.inacap.fyagobras.domain.factura.Factura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    boolean existsByNumeroFacturaIgnoreCase(String numeroFactura);

    Page<Factura> findByEstadoPago(EstadoPago estadoPago, Pageable pageable);
    boolean existsByObraIdAndEstadoPago(Long obraId, EstadoPago estadoPago);

    @Query("""
            select c.id as clienteId, c.rut as rut, c.razonSocial as razonSocial,
                   count(f) as facturasPendientes,
                   sum(f.monto) as montoAdeudado,
                   min(f.fechaVencimiento) as vencimientoMasAntiguo
            from Factura f
            join f.obra o
            join o.presupuesto p
            join p.cliente c
            where f.estadoPago = com.inacap.fyagobras.domain.factura.EstadoPago.PENDIENTE
              and f.anulada = false
              and (lower(c.razonSocial) like lower(concat('%', :busqueda, '%'))
                   or lower(c.rut) like lower(concat('%', :busqueda, '%')))
            group by c.id, c.rut, c.razonSocial
            order by min(f.fechaVencimiento) asc
            """)
    List<ClientePendienteProyeccion> clientesConPagosPendientes(@Param("busqueda") String busqueda);
}