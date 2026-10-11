package cl.duoc.pedidos360.ms_rabbit_admin.controller;

import cl.duoc.pedidos360.ms_rabbit_admin.dto.BindingRequest;
import cl.duoc.pedidos360.ms_rabbit_admin.dto.ExchangeRequest;
import cl.duoc.pedidos360.ms_rabbit_admin.dto.PublishRequest;
import cl.duoc.pedidos360.ms_rabbit_admin.dto.QueueInfoResponse;
import cl.duoc.pedidos360.ms_rabbit_admin.dto.QueueRequest;
import cl.duoc.pedidos360.ms_rabbit_admin.service.RabbitAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API REST de administracion de RabbitMQ.
 *
 *   POST   /queues                 crea una cola
 *   GET    /queues/{name}          estado de la cola (mensajes y consumidores)
 *   DELETE /queues/{name}          elimina la cola
 *   POST   /queues/{name}/purge    vacia la cola
 *   POST   /exchanges              crea un exchange
 *   DELETE /exchanges/{name}       elimina el exchange
 *   POST   /bindings               crea un binding
 *   DELETE /bindings?queue=&exchange=&routingKey=   elimina un binding
 *   POST   /messages               publica un mensaje de prueba
 */
@RestController
@RequiredArgsConstructor
public class RabbitAdminController {

    private final RabbitAdminService service;

    // ------------------------------------------------------------------ colas
    @PostMapping("/queues")
    public ResponseEntity<QueueInfoResponse> crearCola(@Valid @RequestBody QueueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearCola(request));
    }

    @GetMapping("/queues/{name}")
    public QueueInfoResponse obtenerCola(@PathVariable String name) {
        return service.obtenerCola(name);
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> eliminarCola(@PathVariable String name) {
        service.eliminarCola(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/queues/{name}/purge")
    public Map<String, Object> purgarCola(@PathVariable String name) {
        return Map.of("queue", name, "mensajesEliminados", service.purgarCola(name));
    }

    // ------------------------------------------------------------------ exchanges
    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, String>> crearExchange(@Valid @RequestBody ExchangeRequest request) {
        service.crearExchange(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("exchange", request.getName(), "type", request.getType().name()));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String name) {
        service.eliminarExchange(name);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ bindings
    @PostMapping("/bindings")
    public ResponseEntity<BindingRequest> crearBinding(@Valid @RequestBody BindingRequest request) {
        service.crearBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid BindingRequest request) {
        service.eliminarBinding(request);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ mensajes de prueba
    @PostMapping("/messages")
    public ResponseEntity<Map<String, String>> publicar(@Valid @RequestBody PublishRequest request) {
        service.publicar(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("estado", "publicado", "exchange", request.getExchange(),
                        "routingKey", request.getRoutingKey()));
    }
}
