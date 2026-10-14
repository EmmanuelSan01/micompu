package co.edu.unab.micompu.controller;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejo de los códigos de respuesta HTTP de la API:
 *
 * - 400 (Bad Request): petición con datos inválidos, incompletos o
 *   con un cuerpo ilegible (JSON mal formado o de tipos incompatibles).
 * - 404 (Not Found): recurso solicitado que no existe (o inactivo
 *   por borrado lógico).
 * - 409 (Conflict): conflicto de negocio (duplicados, cruces de
 *   horario) o restricciones de integridad de la base de datos.
 *
 * Política de mensajes: las excepciones de negocio ({@code IllegalArgument…},
 * {@code NoSuchElement…}, {@code IllegalState…}) se lanzan en los services con
 * mensajes redactados para el cliente y se responden tal cual; en cambio, las
 * de frameworks y librerías (Jackson, JDBC) pueden filtrar detalles internos
 * —nombres de tablas y restricciones, trazas del parser, fragmentos del
 * payload— y se responden con mensajes controlados, nunca con su mensaje
 * original.
 *
 * El resto de errores no controlados (p. ej. fallos de conexión con MySQL)
 * los maneja Spring Boot con su respuesta por defecto (500).
 */
@RestControllerAdvice
public class ManejadorErrores {

    /** 404: recurso no encontrado (o inactivo por borrado lógico). */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(NoSuchElementException e) {
        // Excepción de negocio: su mensaje se redacta en los services.
        return responder(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** 400: datos de la petición inválidos o incompletos. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> peticionInvalida(IllegalArgumentException e) {
        // Excepción de negocio: su mensaje se redacta en los services.
        return responder(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** 409: conflicto de negocio (duplicados, cruces de horario). */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> conflicto(IllegalStateException e) {
        // Excepción de negocio: su mensaje se redacta en los services.
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    /** 400: cuerpo de la petición ilegible. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> cuerpoIlegible(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición es ilegible: revise el formato JSON y los tipos de datos.");
    }

    /** 409: violación de integridad de la base de datos. */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> violacionIntegridad(
            SQLIntegrityConstraintViolationException e) {
        return responder(HttpStatus.CONFLICT,
                "La operación viola una restricción de integridad de la base de datos.");
    }

    /**
     * Cuerpo de error JSON con la forma {estado, error, mensaje}: el mensaje lo
     * decide cada handler de forma explícita.
     */
    private ResponseEntity<Map<String, Object>> responder(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of(
                "estado", estado.value(),
                "error", estado.getReasonPhrase(),
                "mensaje", mensaje != null && !mensaje.isBlank() ? mensaje : estado.getReasonPhrase()));
    }
}
