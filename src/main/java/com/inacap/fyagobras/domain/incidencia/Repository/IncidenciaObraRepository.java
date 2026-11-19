package com.inacap.fyagobras.domain.incidencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidenciaObraRepository extends JpaRepository<IncidenciaObra, Long> {

    List<IncidenciaObra> findByObraIdOrderByFechaDesc(Long obraId);
}