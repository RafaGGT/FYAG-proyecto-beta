package com.inacap.fyagobras.domain.costo.repository;

import com.inacap.fyagobras.domain.costo.CostoReal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repositorio CostoRealRepository: define el acceso a datos.
public interface CostoRealRepository extends JpaRepository<CostoReal, Long> {
    // Ejecuta la operacion findByObraId.
    Optional<CostoReal> findByObraId(Long obraId);
    // Ejecuta la operacion existsByObraId.
    boolean existsByObraId(Long obraId);
}
