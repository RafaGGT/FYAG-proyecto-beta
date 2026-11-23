package com.inacap.fyagobras.domain.cliente.repository;

import com.inacap.fyagobras.domain.cliente.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Repositorio ClienteRepository: define el acceso a datos.
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Ejecuta la operacion findByRut.
    Optional<Cliente> findByRut(String rut);

    // HU-01: búsqueda por RUT o razón social, sin distinguir mayúsculas
    @Query("""
            SELECT c FROM Cliente c
            WHERE LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(c.rut) LIKE LOWER(CONCAT('%', :texto, '%'))
            """)
    // Ejecuta la operacion buscar.
    Page<Cliente> buscar(@Param("texto") String texto, Pageable pageable);
}
