package com.inacap.fyagobras.infra.service;


import com.inacap.fyagobras.domain.user.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Marca la clase como un bean de Spring: Spring la crea al arrancar
// y la deja disponible para que Spring Security la use
@Service
// Implementa UserDetailsService: le dice a Spring Security
// "yo sé cómo buscar usuarios en mi base de datos"
public class AuthenticationService implements UserDetailsService {

    // Repositorio JPA para consultar la tabla de usuarios
    // (se prefiere inyección por constructor, pero @Autowired también funciona)
    @Autowired
    private UsuarioRepository repository;

    // Spring Security llama a este método automáticamente durante el login
    // y le pasa el username que la persona escribió
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Busca el usuario en la BD; devuelve un Optional (puede estar vacío)
        return repository.findByUsername(username)
                // Si no existe, lanza esta excepción, que Spring interpreta como login fallido
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

    }
}