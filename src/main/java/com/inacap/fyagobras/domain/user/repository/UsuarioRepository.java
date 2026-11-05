package com.inacap.fyagobras.domain.user.repository;

import com.inacap.fyagobras.domain.user.Role;
import com.inacap.fyagobras.domain.user.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Define la interfaz del repositorio JPA para la entidad Usuario, permitiendo operaciones CRUD y consultas personalizadas
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    // Busca un usuario por su nombre de usuario único, retornando un Optional
    Optional<Usuario> findByUsername(String username);
    
    // Busca un usuario por su correo electrónico, retornando un Optional
    Optional<Usuario> findByEmail(String email);
    
    // Busca un usuario por su RUT, retornando un Optional
    Optional<Usuario> findByRut(String rut);
    
    // Retorna una página de usuarios filtrada por su rol específico
    Page<Usuario> findByRole(Role role, Pageable pageable);
    
    // Retorna una página de usuarios filtrada por su estado activo/inactivo
    Page<Usuario> findByActivo(boolean activo, Pageable pageable);
    
    // Retorna una página de usuarios filtrada simultáneamente por rol y estado activo
    Page<Usuario> findByRoleAndActivo(Role role, boolean activo, Pageable pageable);

    // Retorna una lista de usuarios activos de un rol específico, ordenados alfabéticamente por apellido
    List<Usuario> findByRoleAndActivoTrueOrderByApellidoAsc(Role role);
}