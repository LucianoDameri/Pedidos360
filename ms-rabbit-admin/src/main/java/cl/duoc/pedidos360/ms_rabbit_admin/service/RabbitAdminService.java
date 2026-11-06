package cl.duoc.pedidos360.ms_rabbit_admin.service;

import cl.duoc.pedidos360.ms_rabbit_admin.dto.BindingRequest;
import cl.duoc.pedidos360.ms_rabbit_admin.dto.ExchangeRequest;
import cl.duoc.pedidos360.ms_rabbit_admin.dto.PublishRequest;
import cl.duoc.pedidos360.ms_rabbit_admin.dto.QueueInfoResponse;
import cl.duoc.pedidos360.ms_rabbit_admin.dto.QueueRequest;
import cl.duoc.pedidos360.ms_rabbit_admin.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Toda la logica de administracion de RabbitMQ (colas, exchanges, bindings y
 * publicacion de prueba). Los controladores solo llaman a estos metodos y no
 * conocen la libreria de RabbitMQ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitAdminService {

    private final RabbitAdmin rabbitAdmin;
    private final RabbitTemplate rabbitTemplate;

    // ------------------------------------------------------------------ colas
    public QueueInfoResponse crearCola(QueueRequest req) {
        if (tieneTexto(req.getDeadLetterRoutingKey()) && !tieneTexto(req.getDeadLetterExchange())) {
            throw new IllegalArgumentException("deadLetterRoutingKey requiere indicar deadLetterExchange");
        }
        QueueBuilder builder = req.isDurable()
                ? QueueBuilder.durable(req.getName())
                : QueueBuilder.nonDurable(req.getName());
        if (tieneTexto(req.getDeadLetterExchange())) {
            builder.deadLetterExchange(req.getDeadLetterExchange());
        }
        if (tieneTexto(req.getDeadLetterRoutingKey())) {
            builder.deadLetterRoutingKey(req.getDeadLetterRoutingKey());
        }
        if (req.getMessageTtlMs() != null) {
            builder.ttl(req.getMessageTtlMs());
        }
        if (req.getMaxLength() != null) {
            builder.maxLength(req.getMaxLength());
        }
        Queue queue = builder.build();
        rabbitAdmin.declareQueue(queue);
        log.info("[ADMIN] Cola creada: {}", req.getName());
        return new QueueInfoResponse(req.getName(), 0, 0);
    }

    public QueueInfoResponse obtenerCola(String nombre) {
        var info = rabbitAdmin.getQueueInfo(nombre);
        if (info == null) {
            throw new RecursoNoEncontradoException("La cola '" + nombre + "' no existe");
        }
        return new QueueInfoResponse(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    public void eliminarCola(String nombre) {
        obtenerCola(nombre); // 404 si no existe
        rabbitAdmin.deleteQueue(nombre);
        log.info("[ADMIN] Cola eliminada: {}", nombre);
    }

    public int purgarCola(String nombre) {
        obtenerCola(nombre); // 404 si no existe
        int eliminados = rabbitAdmin.purgeQueue(nombre);
        log.info("[ADMIN] Cola purgada: {} ({} mensajes eliminados)", nombre, eliminados);
        return eliminados;
    }

    // ------------------------------------------------------------------ exchanges
    public void crearExchange(ExchangeRequest req) {
        Exchange exchange = switch (req.getType()) {
            case DIRECT -> new DirectExchange(req.getName(), req.isDurable(), false);
            case TOPIC -> new TopicExchange(req.getName(), req.isDurable(), false);
            case FANOUT -> new FanoutExchange(req.getName(), req.isDurable(), false);
            case HEADERS -> new HeadersExchange(req.getName(), req.isDurable(), false);
        };
        rabbitAdmin.declareExchange(exchange);
        log.info("[ADMIN] Exchange creado: {} ({})", req.getName(), req.getType());
    }

    public void eliminarExchange(String nombre) {
        exigirExchange(nombre);
        rabbitAdmin.deleteExchange(nombre);
        log.info("[ADMIN] Exchange eliminado: {}", nombre);
    }

    // ------------------------------------------------------------------ bindings
    public void crearBinding(BindingRequest req) {
        exigirDestinoYOrigen(req);
        rabbitAdmin.declareBinding(construirBinding(req));
        log.info("[ADMIN] Binding creado: {} <- {} [{}]", req.getQueue(), req.getExchange(), req.getRoutingKey());
    }

    public void eliminarBinding(BindingRequest req) {
        exigirDestinoYOrigen(req);
        rabbitAdmin.removeBinding(construirBinding(req));
        log.info("[ADMIN] Binding eliminado: {} <- {} [{}]", req.getQueue(), req.getExchange(), req.getRoutingKey());
    }

    // ------------------------------------------------------------------ publicacion de prueba
    public void publicar(PublishRequest req) {
        Message mensaje = MessageBuilder
                .withBody(req.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .build();
        rabbitTemplate.send(req.getExchange(), req.getRoutingKey(), mensaje);
        log.info("[ADMIN] Mensaje publicado en exchange '{}' con routing key '{}'",
                req.getExchange(), req.getRoutingKey());
    }

    // ------------------------------------------------------------------ privados
    private Binding construirBinding(BindingRequest req) {
        String routingKey = req.getRoutingKey() != null ? req.getRoutingKey() : "";
        return new Binding(req.getQueue(), Binding.DestinationType.QUEUE, req.getExchange(), routingKey, null);
    }

    private void exigirDestinoYOrigen(BindingRequest req) {
        obtenerCola(req.getQueue());
        exigirExchange(req.getExchange());
    }

    private void exigirExchange(String nombre) {
        if (!existeExchange(nombre)) {
            throw new RecursoNoEncontradoException("El exchange '" + nombre + "' no existe");
        }
    }

    /** Declaracion pasiva: no crea nada, solo comprueba si el exchange existe. */
    private boolean existeExchange(String nombre) {
        return Boolean.TRUE.equals(rabbitTemplate.execute(channel -> {
            try {
                channel.exchangeDeclarePassive(nombre);
                return true;
            } catch (java.io.IOException e) {
                return false; // el broker cierra el canal cuando el exchange no existe
            }
        }));
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
