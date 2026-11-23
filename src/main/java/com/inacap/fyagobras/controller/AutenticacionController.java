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
// Este controlador recibe usuario y contraseña, verifica la identidad y entrega tokens JWT.
// Responde con datos de autenticación en lugar de redirigir a una página HTML.
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
        
        // @RequestBody convierte el JSON enviado por el cliente en un objeto LoginDTO.
        // @Valid comprueba primero las reglas de validación declaradas dentro del DTO.
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

        // ResponseEntity contiene el estado HTTP, los encabezados y el cuerpo de la respuesta.
        // ok() asigna 200 OK y el objeto DatosJWTToken se convierte automáticamente a JSON.
        return ResponseEntity.ok(new DatosJWTToken(accessToken, refreshToken));
    }

    // Define un endpoint POST en "/login/actualizar-token" para renovar las credenciales
    @PostMapping("/actualizar-token")
    public ResponseEntity<DatosJWTToken> actualizarToken(@RequestBody String refreshToken) {
        
        // @RequestBody recibe el refresh token como texto enviado por el cliente.
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

        // Si todas las comprobaciones pasan, 200 OK indica que la renovación fue exitosa.
        // El cuerpo contiene el nuevo access token y un nuevo refresh token.
        return ResponseEntity.ok(new DatosJWTToken(nuevoAccessToken, nuevoRefreshToken));
    }
}