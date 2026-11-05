package com.inacap.fyagobras.domain.servicio.repository;

import com.inacap.fyagobras.domain.servicio.TipoServicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoServicioRepository extends JpaRepository<TipoServicio, Long> {

    List<TipoServicio> findByActivoTrueOrderByNombreAsc();

    List<TipoServicio> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}