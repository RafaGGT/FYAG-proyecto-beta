package com.inacap.fyagobras.domain.audit.repository;

import com.inacap.fyagobras.domain.audit.LogAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;

// Repositorio MongoDB para consultar y almacenar registros de auditoría.
public interface AuditoriaRepository extends MongoRepository<LogAuditoria, String> {

    // Busca auditorías asociadas al identificador de un usuario.
    Page<LogAuditoria> findByUsuarioId(Long usuarioId, Pageable paginacion);

    // Busca auditorías cuya fecha esté dentro del rango indicado.
    Page<LogAuditoria> findByFechaBetween(Instant desde, Instant hasta, Pageable paginacion);
}
