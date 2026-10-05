package cl.duoc.pedidos360.ms_bff.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    /** Un microservicio respondió con error: se propaga el mismo código al frontend. */
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, Object>> downstreamError(RestClientResponseException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("error", "Error en microservicio", "status", e.getStatusCode().value()));
    }

    /** Un microservicio está caído o no responde. */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> downstreamDown(ResourceAccessException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Microservicio no disponible", "status", 502));
    }
}
