package com.inacap.fyagobras.domain.obra.repository;

import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.obra.Obra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

// Repositorio ObraRepository: define el acceso a datos.
public interface ObraRepository extends JpaRepository<Obra, Long>, JpaSpecificationExecutor<Obra> {

    // Ejecuta la operacion existsByPresupuestoId.
    boolean existsByPresupuestoId(Long presupuestoId);

    // Ejecuta la operacion findByPresupuestoId.
    Optional<Obra> findByPresupuestoId(Long presupuestoId);

    // Ejecuta la operacion findByEstado.
    Page<Obra> findByEstado(EstadoObra estado, Pageable pageable);

    // Ejecuta la operacion findByTecnicoId.
    Page<Obra> findByTecnicoId(Long tecnicoId, Pageable pageable);

    // Ejecuta la operacion findByTecnicoIdAndEstado.
    Page<Obra> findByTecnicoIdAndEstado(Long tecnicoId, EstadoObra estado, Pageable pageable);

    // Ejecuta la operacion countByEstado.
    long countByEstado(EstadoObra estado);
}
