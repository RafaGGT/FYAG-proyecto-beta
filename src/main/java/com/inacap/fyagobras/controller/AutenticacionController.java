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

@RestController
@RequestMapping("/login")
public class AutenticacionController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository userRepository;

    // POST /login: valida usuario y contraseña y entrega access token + refresh token
    @PostMapping
    public ResponseEntity<DatosJWTToken> autenticarUsuario(
            @RequestBody @Valid LoginDTO datosAutenticacionUsuario) {
        Authentication authToken = new UsernamePasswordAuthenticationToken(
                datosAutenticacionUsuario.username(),
                datosAutenticacionUsuario.password());
        var usuarioAutenticado = authenticationManager.authenticate(authToken);
        var usuario = (Usuario) usuarioAutenticado.getPrincipal();

        var accessToken = tokenService.generarToken(usuario);
        var refreshToken = tokenService.generarRefreshToken(usuario);

        return ResponseEntity.ok(new DatosJWTToken(accessToken, refreshToken));
    }

    // POST /actualizar-token: actualiza el access token usando el refresh token
    @PostMapping("/actualizar-token")
    public ResponseEntity<DatosJWTToken> actualizarToken(@RequestBody String refreshToken) {
        var decoded = tokenService.verificarToken(refreshToken, "refresh");
        var idUsuario = Long.valueOf(decoded.getSubject());
        var usuario = (Usuario) userRepository.findById(idUsuario)
                .orElseThrow(() -> new TokenInvalidoException("Usuario del token no existe", null));

        // Verifica que el usuario esté habilitado
        if (!usuario.isEnabled()) {
            throw new TokenInvalidoException("Usuario desactivado", null);
        }

        var nuevoAccessToken = tokenService.generarToken(usuario);
        var nuevoRefreshToken = tokenService.generarRefreshToken(usuario);

        return ResponseEntity.ok(new DatosJWTToken(nuevoAccessToken, nuevoRefreshToken));
    }
}