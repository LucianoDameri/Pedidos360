package cl.duoc.pedidos360.ms_pedidos.messaging.publisher;

import cl.duoc.pedidos360.ms_pedidos.config.MessagingProperties;
import cl.duoc.pedidos360.ms_pedidos.messaging.event.PedidoEvent;
import cl.duoc.pedidos360.ms_pedidos.model.Pedido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Productor de eventos de pedido. Si RabbitMQ no esta disponible solo se
 * registra el error: la compra del cliente no debe fallar por la mensajeria.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PedidoEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties props;

    public void pedidoCreado(Pedido pedido) {
        publicar(props.getRoutingKeys().getPedidoCreado(), PedidoEvent.desde("PEDIDO_CREADO", pedido));
    }

    public void pedidoCancelado(Pedido pedido) {
        publicar(props.getRoutingKeys().getPedidoCancelado(), PedidoEvent.desde("PEDIDO_CANCELADO", pedido));
    }

    private void publicar(String routingKey, PedidoEvent evento) {
        try {
            rabbitTemplate.convertAndSend(props.getExchanges().getPedidos(), routingKey, evento);
            log.info("[RABBIT] Publicado {} (pedidoId={}) en {} con routing key {}",
                    evento.tipo(), evento.pedidoId(), props.getExchanges().getPedidos(), routingKey);
        } catch (AmqpException e) {
            log.error("[RABBIT] No se pudo publicar {} (pedidoId={}). El pedido sigue valido: {}",
                    evento.tipo(), evento.pedidoId(), e.getMessage());
        }
    }
}
