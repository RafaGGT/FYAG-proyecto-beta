package com.inacap.fyagobras.domain.incidencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Repositorio IncidenciaObraRepository: define el acceso a datos.
public interface IncidenciaObraRepository extends JpaRepository<IncidenciaObra, Long> {

    // Ejecuta la operacion findByObraIdOrderByFechaDesc.
    List<IncidenciaObra> findByObraIdOrderByFechaDesc(Long obraId);
}
