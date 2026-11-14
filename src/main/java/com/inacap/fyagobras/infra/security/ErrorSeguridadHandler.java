package com.inacap.fyagobras.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Genera las respuestas HTTP para solicitudes no autenticadas o sin permisos.
 *
 * <p>Implementa los puntos de entrada que Spring Security utiliza cuando no
 * existe una autenticación válida o cuando el usuario autenticado no puede
 * acceder al recurso solicitado.</p>
 */
@Component
public class ErrorSeguridadHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    /**
     * Responde cuando la solicitud no contiene una autenticación válida.
     *
     * @param req solicitud HTTP rechazada
     * @param res respuesta HTTP que se enviará al cliente
     * @param e excepción que describe el fallo de autenticación
     * @throws IOException si no es posible escribir la respuesta
     */
    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res,
                         AuthenticationException e) throws IOException {
        escribir(res, 401, "No autenticado");
    }

    /**
     * Responde cuando el usuario está autenticado, pero no tiene permisos.
     *
     * @param req solicitud HTTP rechazada
     * @param res respuesta HTTP que se enviará al cliente
     * @param e excepción que describe el acceso denegado
     * @throws IOException si no es posible escribir la respuesta
     */
    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res,
                       AccessDeniedException e) throws IOException {
        escribir(res, 403, "Sin permisos para esta acción");
    }

    /**
     * Escribe un error con formato compatible con {@code application/problem+json}.
     *
     * @param res respuesta HTTP que se configurará
     * @param status código de estado HTTP
     * @param detalle mensaje que se devolverá en el campo {@code detail}
     * @throws IOException si no es posible escribir el cuerpo de la respuesta
     */
    public void escribir(HttpServletResponse res, int status, String detalle) throws IOException {
        res.setStatus(status);
        res.setContentType("application/problem+json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + detalle + "\"}");
    }
}