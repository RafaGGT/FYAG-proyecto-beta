package com.inacap.fyagobras.domain.notificacion.repository;

import com.inacap.fyagobras.domain.notificacion.Notificacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByDestinatarioIdOrderByFechaCreacionDesc(Long destinatarioId, Pageable pageable);

    long countByDestinatarioIdAndLeidaFalse(Long destinatarioId);

    // Buscar por id Y destinatario evita que un usuario toque avisos ajenos
    Optional<Notificacion> findByIdAndDestinatarioId(Long id, Long destinatarioId);

    @Modifying
    @Query("update Notificacion n set n.leida = true where n.destinatarioId = :usuarioId and n.leida = false")
    int marcarTodasLeidas(@Param("usuarioId") Long usuarioId);
}