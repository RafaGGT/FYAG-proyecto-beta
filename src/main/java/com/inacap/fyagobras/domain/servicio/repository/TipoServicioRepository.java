package com.inacap.fyagobras.domain.servicio.repository;

import com.inacap.fyagobras.domain.servicio.TipoServicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Repositorio TipoServicioRepository: define el acceso a datos.
public interface TipoServicioRepository extends JpaRepository<TipoServicio, Long> {

    // Ejecuta la operacion findByActivoTrueOrderByNombreAsc.
    List<TipoServicio> findByActivoTrueOrderByNombreAsc();

    // Ejecuta la operacion findAllByOrderByNombreAsc.
    List<TipoServicio> findAllByOrderByNombreAsc();

    // Ejecuta la operacion existsByNombreIgnoreCase.
    boolean existsByNombreIgnoreCase(String nombre);

    // Ejecuta la operacion existsByNombreIgnoreCaseAndIdNot.
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
