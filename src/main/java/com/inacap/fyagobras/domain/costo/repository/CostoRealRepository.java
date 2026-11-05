package com.inacap.fyagobras.domain.costo.repository;

import com.inacap.fyagobras.domain.costo.CostoReal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CostoRealRepository extends JpaRepository<CostoReal, Long> {
    Optional<CostoReal> findByObraId(Long obraId);
}