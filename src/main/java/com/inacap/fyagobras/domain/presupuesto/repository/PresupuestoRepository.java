package com.inacap.fyagobras.domain.presupuesto.repository;

import com.inacap.fyagobras.domain.presupuesto.EstadoPresupuesto;
import com.inacap.fyagobras.domain.presupuesto.Presupuesto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {

    Page<Presupuesto> findByEstado(EstadoPresupuesto estado, Pageable pageable);

    List<Presupuesto> findByEstadoOrderByFechaAprobacionDesc(EstadoPresupuesto estado);

    // Número correlativo que entrega la secuencia de PostgreSQL
    @Query(value = "SELECT nextval('seq_numero_presupuesto')", nativeQuery = true)
    Long siguienteNumero();

    long countByEstado(EstadoPresupuesto estado);
}