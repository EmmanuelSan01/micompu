package co.edu.unab.micompu.controller;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejo básico de los códigos de respuesta HTTP de la API:
 *
 * <ul>
 *   <li><b>400</b> (Bad Request): petición con datos inválidos o incompletos.</li>
 *   <li><b>404</b> (Not Found): recurso solicitado que no existe.</li>
 *   <li><b>409</b> (Conflict): conflicto de negocio (duplicados, cruces de
 *   horario o restricciones de integridad de la base de datos).</li>
 * </ul>
 *
 * <p>El resto de errores no controlados (p. ej. fallos de conexión con MySQL)
 * los maneja Spring Boot con su respuesta por defecto (500).</p>
 */
@RestControllerAdvice
public class ManejadorErrores {

    /** 404: recurso no encontrado. */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(NoSuchElementException e) {
        return responder(HttpStatus.NOT_FOUND, e);
    }

    /** 400: datos de la petición inválidos o incompletos. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> peticionInvalida(IllegalArgumentException e) {
        return responder(HttpStatus.BAD_REQUEST, e);
    }

    /** 409: conflicto de negocio o restricción de integridad violada. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> conflicto(IllegalStateException e) {
        return responder(HttpStatus.CONFLICT, e);
    }

    private ResponseEntity<Map<String, Object>> responder(HttpStatus estado, RuntimeException e) {
        String mensaje = e.getMessage() != null ? e.getMessage() : estado.getReasonPhrase();
        return ResponseEntity.status(estado).body(Map.of(
                "estado", estado.value(),
                "error", estado.getReasonPhrase(),
                "mensaje", mensaje));
    }
}
