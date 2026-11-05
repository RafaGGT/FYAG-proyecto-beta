package com.inacap.fyagobras.controller;

import com.inacap.fyagobras.domain.user.Usuario;
import com.inacap.fyagobras.domain.user.dto.LoginDTO;
import com.inacap.fyagobras.domain.user.repository.UsuarioRepository;
import com.inacap.fyagobras.infra.exception.TokenInvalidoException;
import com.inacap.fyagobras.infra.security.DatosJWTToken;
import com.inacap.fyagobras.infra.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Define la clase como un controlador REST y mapea la ruta base "/login"
@RestController
@RequestMapping("/login")
public class AutenticacionController {

    // Inyecta el administrador de autenticación de Spring Security
    @Autowired
    private AuthenticationManager authenticationManager;

    // Inyecta el servicio encargado de generar y verificar tokens JWT
    @Autowired
    private TokenService tokenService;

    // Inyecta el repositorio para realizar operaciones con la entidad Usuario en la base de datos
    @Autowired
    private UsuarioRepository userRepository;

    // Define un endpoint POST en la raíz ("/login") para autenticar usuarios
    @PostMapping
    public ResponseEntity<DatosJWTToken> autenticarUsuario(
            @RequestBody @Valid LoginDTO datosAutenticacionUsuario) {
        
        // Crea un token de autenticación temporal con las credenciales recibidas (username y password)
        Authentication authToken = new UsernamePasswordAuthenticationToken(
                datosAutenticacionUsuario.username(),
                datosAutenticacionUsuario.password());
        
        // Autentica al usuario utilizando el AuthenticationManager
        var usuarioAutenticado = authenticationManager.authenticate(authToken);
        
        // Extrae el objeto Usuario autenticado desde el principal
        var usuario = (Usuario) usuarioAutenticado.getPrincipal();

        // Genera el token de acceso (Access Token) para el usuario
        var accessToken = tokenService.generarToken(usuario);
        
        // Genera el token de refresco (Refresh Token) para renovar el acceso posteriormente
        var refreshToken = tokenService.generarRefreshToken(usuario);

        // Retorna una respuesta HTTP 200 OK con ambos tokens
        return ResponseEntity.ok(new DatosJWTToken(accessToken, refreshToken));
    }

    // Define un endpoint POST en "/login/actualizar-token" para renovar las credenciales
    @PostMapping("/actualizar-token")
    public ResponseEntity<DatosJWTToken> actualizarToken(@RequestBody String refreshToken) {
        
        // Verifica y decodifica el refresh token recibido asegurando que sea de tipo "refresh"
        var decoded = tokenService.verificarToken(refreshToken, "refresh");
        
        // Extrae el ID del usuario desde el subject del token decodificado
        var idUsuario = Long.valueOf(decoded.getSubject());
        
        // Busca al usuario en la base de datos; si no existe, lanza una excepción
        var usuario = (Usuario) userRepository.findById(idUsuario)
                .orElseThrow(() -> new TokenInvalidoException("Usuario del token no existe", null));

        // Valida si el usuario se encuentra activo/habilitado en el sistema
        if (!usuario.isEnabled()) {
            throw new TokenInvalidoException("Usuario desactivado", null);
        }

        // Genera un nuevo access token
        var nuevoAccessToken = tokenService.generarToken(usuario);
        
        // Genera un nuevo refresh token
        var nuevoRefreshToken = tokenService.generarRefreshToken(usuario);

        // Retorna una respuesta HTTP 200 OK con los nuevos tokens renovados
        return ResponseEntity.ok(new DatosJWTToken(nuevoAccessToken, nuevoRefreshToken));
    }
}