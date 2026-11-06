package cl.duoc.pedidos360.ms_rabbit_admin.exception;

import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Respuestas de error claras en JSON: {"error": "mensaje"}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(RecursoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> argumentoInvalido(IllegalArgumentException e) {
        return respuesta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Falla de validacion de los DTO (@Valid): cuerpo JSON o parametros de la URL. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> validacion(BindException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getDefaultMessage())
                .orElse("Datos invalidos");
        return respuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> jsonInvalido(HttpMessageNotReadableException e) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "El cuerpo de la peticion no es un JSON valido (revisa tipos como DIRECT/TOPIC/FANOUT/HEADERS)");
    }

    /** RabbitMQ no responde (nodo caido o credenciales incorrectas). */
    @ExceptionHandler(AmqpConnectException.class)
    public ResponseEntity<Map<String, String>> sinConexion(AmqpConnectException e) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "No hay conexion con RabbitMQ: " + e.getMessage());
    }

    /** RabbitMQ rechazo la operacion (por ejemplo, la cola ya existe con otros argumentos). */
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Map<String, String>> rechazadoPorRabbit(AmqpException e) {
        Throwable causa = e.getCause() != null ? e.getCause() : e;
        return respuesta(HttpStatus.CONFLICT, "RabbitMQ rechazo la operacion: " + causa.getMessage());
    }

    private ResponseEntity<Map<String, String>> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of("error", String.valueOf(mensaje)));
    }
}
