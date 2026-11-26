package com.inacap.fyagobras.domain.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Valida los parámetros de consulta (query string) de los endpoints de auditoría.
 *
 * <p>Spring ignora en silencio los parámetros que el endpoint no declara, por lo que
 * una consulta como {@code ?usuarioId[$ne]=0} terminaba devolviendo todos los registros
 * con 200. Aquí se aplica una lista blanca: cualquier parámetro desconocido, o con
 * caracteres de operador NoSQL, se rechaza con 400 (OWASP A03).</p>
 */
public final class ParametrosConsulta {

    private static final Set<String> PAGINACION = Set.of("page", "size", "sort");
    // Solo se permite ordenar por campos conocidos de LogAuditoria.
    private static final Set<String> CAMPOS_ORDEN =
            Set.of("fecha", "usuarioId", "usuarioRol", "entidad", "entidadId", "accion");

    private ParametrosConsulta() {}

    /**
     * @param request solicitud actual
     * @param extra   parámetros adicionales permitidos para ese endpoint (ej: "desde", "hasta")
     * @throws ValidationException (se traduce a 400 en GestorErrores) si hay algo no permitido
     */
    public static void validar(HttpServletRequest request, String... extra) {
        Set<String> permitidos = new HashSet<>(PAGINACION);
        permitidos.addAll(Arrays.asList(extra));

        request.getParameterMap().forEach((nombre, valores) -> {
            if (!permitidos.contains(nombre)) {
                throw new ValidationException("Parámetro no permitido");
            }
            for (String valor : valores) {
                if (valor.contains("$") || valor.contains("{") || valor.contains("}")) {
                    throw new ValidationException("Valor no permitido");
                }
            }
        });

        String[] orden = request.getParameterValues("sort");
        if (orden != null) {
            for (String s : orden) {
                String campo = s.split(",")[0].trim();
                if (!CAMPOS_ORDEN.contains(campo)) {
                    throw new ValidationException("Campo de orden no permitido");
                }
            }
        }
    }
}