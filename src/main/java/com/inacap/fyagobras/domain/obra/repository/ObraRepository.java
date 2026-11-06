package com.inacap.fyagobras.domain.obra.repository;

import com.inacap.fyagobras.domain.obra.EstadoObra;
import com.inacap.fyagobras.domain.obra.Obra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ObraRepository extends JpaRepository<Obra, Long>, JpaSpecificationExecutor<Obra> {

    boolean existsByPresupuestoId(Long presupuestoId);

    Page<Obra> findByEstado(EstadoObra estado, Pageable pageable);

    Page<Obra> findByTecnicoId(Long tecnicoId, Pageable pageable);

    Page<Obra> findByTecnicoIdAndEstado(Long tecnicoId, EstadoObra estado, Pageable pageable);

    long countByEstado(EstadoObra estado);
}