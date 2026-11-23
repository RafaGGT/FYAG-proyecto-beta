package com.inacap.fyagobras.infra.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import jakarta.validation.ValidationException;
import java.util.stream.Collectors;


/**
 * Centraliza la traducción de excepciones de la aplicación a respuestas HTTP
 * con formato {@link ProblemDetail}.
 *
 * <p>Los errores controlados exponen un estado y un mensaje apropiados para el
 * cliente, mientras que las excepciones no controladas se registran en el
 * servidor y se responden con un mensaje genérico.</p>
 */
@RestControllerAdvice
// Esta clase centraliza los errores de todos los controladores REST.
// En vez de repetir la respuesta de error en cada endpoint, aquí se decide qué HTTP status devolver.
public class GestorErrores extends ResponseEntityExceptionHandler {

    // Logger usado para escribir errores técnicos en la consola o los registros del servidor.
    private static final Logger log = LoggerFactory.getLogger(GestorErrores.class);

    /**
     * Convierte errores de Bean Validation que no pasan por {@code @Valid} en
     * una respuesta {@code 400 Bad Request}.
     *
     * @param e excepción de validación capturada
     * @return detalle del problema con un mensaje genérico
     */
    @ExceptionHandler(ValidationException.class)
    public ProblemDetail validacion(ValidationException e) {
        // 400 Bad Request significa que el cliente envió datos que no pasan una validación.
        // La respuesta no entrega detalles internos y muestra un mensaje simple al usuario.
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos inválidos");
    }

    /**
     * Responde cuando el recurso solicitado no existe.
     *
     * @param e excepción que contiene el detalle del recurso no encontrado
     * @return detalle del problema con estado {@code 404 Not Found}
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail noEncontrado(RecursoNoEncontradoException e) {
        // 404 Not Found significa que la búsqueda no encontró el recurso solicitado.
        // Se reutiliza el mensaje enviado por la excepción para explicar qué recurso faltó.
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /**
     * Responde cuando una operación incumple una regla de negocio.
     *
     * @param e excepción que contiene el motivo del conflicto
     * @return detalle del problema con estado {@code 409 Conflict}
     */
    @ExceptionHandler(ConflictoNegocioException.class)
    public ProblemDetail conflicto(ConflictoNegocioException e) {
        // 409 Conflict indica que los datos existen, pero la operación contradice una regla de negocio.
        // Por ejemplo: pagar una factura ya pagada o finalizar una obra sin costos.
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /**
     * Traduce una violación de integridad de datos a un conflicto HTTP y
     * registra la causa específica para diagnóstico interno.
     *
     * @param e excepción de integridad producida por la persistencia
     * @return detalle del problema con estado {@code 409 Conflict}
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridad(DataIntegrityViolationException e) {
        // Obtiene la causa más específica para escribirla en el log del servidor.
        // Esta causa puede contener detalles técnicos y no se muestra directamente al usuario.
        log.warn("Violación de integridad: {}", e.getMostSpecificCause().getMessage());
        // 409 Conflict resume el problema como registro duplicado o restricción incumplida.
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "El registro ya existe o viola una restricción");
    }

    /**
     * Responde ante fallos de autenticación sin revelar si fallaron las
     * credenciales o el estado de la cuenta.
     *
     * @param e excepción de autenticación capturada
     * @return detalle del problema con estado {@code 401 Unauthorized}
     */
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail autenticacion(AuthenticationException e) {
        // 401 Unauthorized indica que el usuario no presentó credenciales válidas.
        // El mensaje deliberadamente no revela si falló el usuario, la contraseña o el estado de la cuenta.
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
    }

    /**
     * Responde ante un token inválido, expirado o asociado a un usuario no
     * disponible.
     *
     * @param e excepción relacionada con la validación del token
     * @return detalle del problema con estado {@code 401 Unauthorized}
     */
    @ExceptionHandler(TokenInvalidoException.class)
    public ProblemDetail token(TokenInvalidoException e) {
        // 401 Unauthorized indica que el token no existe, expiró o pertenece a una cuenta inválida.
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Token inválido o expirado");
    }

    /**
     * Conserva el estado de autorización cuando un usuario autenticado no
     * tiene permisos para una operación protegida.
     *
     * @param e excepción de acceso denegado capturada
     * @return detalle del problema con estado {@code 403 Forbidden}
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accesoDenegado(AccessDeniedException e) {
        // 403 Forbidden indica una identidad válida que no tiene el permiso necesario.
        // Es distinto de 401: aquí sí se reconoce al usuario, pero su rol no permite la operación.
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Sin permisos para esta acción");
    }

    /**
     * Maneja cualquier excepción no contemplada y evita exponer detalles
     * internos al cliente.
     *
     * @param e excepción no controlada
     * @return detalle del problema con estado {@code 500 Internal Server Error}
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception e) {
        // Antes de responder, se registra el error completo con su pila de ejecución.
        // Esta información ayuda a los desarrolladores a investigar el problema.
        log.error("Error no controlado", e);
        // 500 Internal Server Error oculta el detalle técnico y evita filtrar información sensible.
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }

    /**
     * Formatea los errores de validación de argumentos de controlador agrupados
     * por nombre de campo.
     *
     * @param ex excepción que contiene los errores de cada campo
     * @param headers cabeceras de la respuesta original
     * @param status estado HTTP original
     * @param request solicitud web que originó el error
     * @return respuesta {@code 400 Bad Request} con la propiedad {@code errores}
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        // getFieldErrors obtiene cada campo que falló una validación del DTO.
        // Se transforma la lista en un Map: nombre del campo como clave y mensaje como valor.
        var errores = ex.getFieldErrors().stream().collect(Collectors.toMap(
                FieldError::getField,
                f -> f.getDefaultMessage() == null ? "inválido" : f.getDefaultMessage(),
                (a, b) -> a));
        // ProblemDetail describe el problema con el formato estándar RFC 7807.
        // status 400 indica que el problema está en los datos enviados por el cliente.
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Datos inválidos");
        // La propiedad personalizada "errores" permite mostrar mensajes exactos junto al campo correspondiente.
        pd.setProperty("errores", errores);
        // ResponseEntity.badRequest() establece 400 Bad Request y body() agrega el objeto ProblemDetail.
        return ResponseEntity.badRequest().body(pd);
    }
}