package com.inacap.fyagobras.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ErrorSeguridadHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res,
                         AuthenticationException e) throws IOException {
        escribir(res, 401, "No autenticado");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res,
                       AccessDeniedException e) throws IOException {
        escribir(res, 403, "Sin permisos para esta acción");
    }

    public void escribir(HttpServletResponse res, int status, String detalle) throws IOException {
        res.setStatus(status);
        res.setContentType("application/problem+json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("{\"status\":" + status + ",\"detail\":\"" + detalle + "\"}");
    }
}