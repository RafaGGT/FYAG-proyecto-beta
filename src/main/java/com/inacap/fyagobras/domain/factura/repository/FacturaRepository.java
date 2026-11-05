package com.inacap.fyagobras.domain.factura.repository;

import com.inacap.fyagobras.domain.factura.EstadoPago;
import com.inacap.fyagobras.domain.factura.Factura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    boolean existsByNumeroFacturaIgnoreCase(String numeroFactura);

    Page<Factura> findByEstadoPago(EstadoPago estadoPago, Pageable pageable);
    boolean existsByObraIdAndEstadoPago(Long obraId, EstadoPago estadoPago);
}