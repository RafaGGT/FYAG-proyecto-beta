package com.inacap.fyagobras.infra.security;

import com.inacap.fyagobras.domain.user.repository.UsuarioRepository;
import com.inacap.fyagobras.infra.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ErrorSeguridadHandler errorSeguridadHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Saltar rutas públicas y preflight OPTIONS

        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(method)
                || path.startsWith("/login")){
            filterChain.doFilter(request, response);
            return;
        }

        var tokenJWT = recuperarToken(request);

        if (tokenJWT == null) {
            errorSeguridadHandler.escribir(response, 401, "No autenticado");
            return;
        }

        try {
            // Extrae el username del subject del token (ya validado en tokenService.getSubject)
            var subject = tokenService.getSubject(tokenJWT);

            var usuarioOpt = repository.findByUsername(subject);
            if (usuarioOpt.isEmpty()) {
                errorSeguridadHandler.escribir(response, 401, "No autenticado");
                return;
            }
            var usuario = usuarioOpt.get();
            if (!usuario.isEnabled()) {
                errorSeguridadHandler.escribir(response, 401, "No autenticado");
                return;
            }
            if (usuario.isDebeCambiarPassword() && !permitidaConCambioPendiente(method, path)) {
                errorSeguridadHandler.escribir(response, 403, "Debe cambiar su contraseña antes de continuar");
                return;
            }

            // Solo si pasó los chequeos anteriores se autentica la request en el contexto de Spring Security
            var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            // Captura firma inválida, token expirado, issuer incorrecto, etc.
            log.debug("Token JWT inválido o expirado: {}", e.getMessage());
            errorSeguridadHandler.escribir(response, 401, "Token inválido o expirado");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.replace("Bearer ", "");
        }
        return null;
    }

    private boolean permitidaConCambioPendiente(String method, String path) {
        return ("GET".equals(method) && path.equals("/usuarios/me"))
                || ("PATCH".equals(method) && path.equals("/usuarios/me/password"));
    }

}