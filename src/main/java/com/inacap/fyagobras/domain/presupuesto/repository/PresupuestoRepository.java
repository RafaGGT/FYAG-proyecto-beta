package com.inacap.fyagobras.domain.presupuesto.repository;

import com.inacap.fyagobras.domain.presupuesto.EstadoPresupuesto;
import com.inacap.fyagobras.domain.presupuesto.Presupuesto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// Repositorio PresupuestoRepository: define el acceso a datos.
public interface PresupuestoRepository extends JpaRepository<Presupuesto, Long> {

    // Ejecuta la operacion findByEstado.
    Page<Presupuesto> findByEstado(EstadoPresupuesto estado, Pageable pageable);

    // Ejecuta la operacion findByEstadoOrderByFechaAprobacionDesc.
    List<Presupuesto> findByEstadoOrderByFechaAprobacionDesc(EstadoPresupuesto estado);

    // Número correlativo que entrega la secuencia de PostgreSQL
    @Query(value = "SELECT nextval('seq_numero_presupuesto')", nativeQuery = true)
    // Ejecuta la operacion siguienteNumero.
    Long siguienteNumero();

    // Ejecuta la operacion countByEstado.
    long countByEstado(EstadoPresupuesto estado);

    // Presupuestos en el estado indicado que todavía no tienen una obra asociada
    @Query("""
            SELECT p FROM Presupuesto p
            WHERE p.estado = :estado
              AND NOT EXISTS (SELECT 1 FROM Obra o WHERE o.presupuesto = p)
            ORDER BY p.fechaAprobacion DESC
            """)
    // Ejecuta la operacion findDisponiblesParaObra.
    List<Presupuesto> findDisponiblesParaObra(@Param("estado") EstadoPresupuesto estado);
}
