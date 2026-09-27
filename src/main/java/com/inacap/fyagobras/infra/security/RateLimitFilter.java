package com.inacap.fyagobras.infra.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Un bucket por IP — se crea la primera vez que esa IP hace un request
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Crea un bucket con el límite configurado
    private Bucket crearBucket() {
        // Máximo 5 intentos cada 1 minuto por IP
        Bandwidth limite = Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limite).build();
    }

    private Bucket obtenerBucket(String ip) {
        return buckets.computeIfAbsent(ip, k -> crearBucket());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();

        // Solo aplica rate limiting a /login
        if (!path.equals("/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Obtiene la IP del cliente
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        Bucket bucket = obtenerBucket(ip);

        if (bucket.tryConsume(1)) {
            // Tiene tokens disponibles → deja pasar
            filterChain.doFilter(request, response);
        } else {
            // Sin tokens → demasiadas peticiones (mismo formato que el resto de errores)
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/problem+json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":429,\"detail\":\"Demasiados intentos. Espera 1 minuto.\"}");
        }
    }
}