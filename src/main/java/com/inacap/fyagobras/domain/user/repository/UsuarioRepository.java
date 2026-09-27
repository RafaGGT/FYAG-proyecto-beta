package com.inacap.fyagobras.domain.user.repository;

import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByEmail(String email);

    Page<Usuario> findByRole(Role role, Pageable pageable);
    Page<Usuario> findByActivo(boolean activo, Pageable pageable);
    Page<Usuario> findByRoleAndActivo(Role role, boolean activo, Pageable pageable);

    List<Usuario> findByRoleAndActivoTrueOrderByApellidoAsc(Role role);
}