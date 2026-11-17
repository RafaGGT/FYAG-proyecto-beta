package com.inacap.fyagobras.domain.audit.repository;

import com.inacap.fyagobras.domain.audit.LogAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;

public interface AuditoriaRepository extends MongoRepository<LogAuditoria, String> {

    Page<LogAuditoria> findByUsuarioId(Long usuarioId, Pageable paginacion);

    Page<LogAuditoria> findByFechaBetween(Instant desde, Instant hasta, Pageable paginacion);
}