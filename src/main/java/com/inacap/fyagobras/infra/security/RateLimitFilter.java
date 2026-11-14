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

/**
 * Limita por dirección IP las solicitudes de inicio de sesión para reducir
 * ataques de fuerza bruta.
 *
 * <p>Aplica un bucket independiente a cada IP y permite como máximo cinco
 * solicitudes por minuto en la ruta {@code /login}.</p>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /** Buckets de consumo asociados a las direcciones IP observadas. */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Crea el limitador para una nueva dirección IP.
     *
     * @return bucket con capacidad para cinco solicitudes por minuto
     */
    private Bucket crearBucket() {
        Bandwidth limite = Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limite).build();
    }

    /**
     * Obtiene el bucket existente o crea uno para la IP indicada.
     *
     * @param ip dirección IP del cliente
     * @return bucket asociado a la IP
     */
    private Bucket obtenerBucket(String ip) {
        return buckets.computeIfAbsent(ip, k -> crearBucket());
    }

    /**
     * Aplica el límite únicamente a {@code /login}; las demás solicitudes
     * continúan por la cadena sin consumir tokens.
     *
     * @param request solicitud HTTP actual
     * @param response respuesta HTTP que puede recibir el rechazo
     * @param filterChain cadena de filtros restante
     * @throws ServletException si falla un filtro posterior
     * @throws IOException si ocurre un error de entrada o salida
     */
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