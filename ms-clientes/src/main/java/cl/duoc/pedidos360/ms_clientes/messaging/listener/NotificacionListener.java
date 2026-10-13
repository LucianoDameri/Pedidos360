package cl.duoc.pedidos360.ms_clientes.messaging.listener;

import cl.duoc.pedidos360.ms_clientes.messaging.event.PagoEvent;
import cl.duoc.pedidos360.ms_clientes.messaging.event.PedidoEvent;
import cl.duoc.pedidos360.ms_clientes.service.NotificacionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consumidores del dominio "notificaciones" (pedido creado/cancelado y pago aprobado).
 *
 * ACK manual y manejo de errores:
 *  - OK                              -> basicAck.
 *  - Error NO recuperable (JSON roto, datos invalidos, cliente inexistente)
 *                                    -> basicNack(requeue=false): va directo a la DLQ.
 *  - Error recuperable (BD caida...) -> un reintento (basicNack requeue=true);
 *                                       si falla de nuevo, basicNack(requeue=false) -> DLQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionListener {

    private final ObjectMapper objectMapper;
    private final NotificacionService notificacionService;

    @RabbitListener(queues = "${app.messaging.queues.notificaciones}")
    public void onNotificacion(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        try {
            // La cola recibe pedidos (pedido.*) y pagos (pago.aprobado): se distingue por la routing key.
            if (routingKey != null && routingKey.startsWith("pedido.")) {
                PedidoEvent evento = leer(message, PedidoEvent.class);
                if (evento == null || evento.clienteId() == null || evento.pedidoId() == null) {
                    throw new IllegalArgumentException("El evento de pedido no trae clienteId/pedidoId");
                }
                notificacionService.notificarPedido(evento);
            } else if (routingKey != null && routingKey.startsWith("pago.")) {
                PagoEvent evento = leer(message, PagoEvent.class);
                if (evento == null || evento.clienteId() == null || evento.pedidoId() == null) {
                    throw new IllegalArgumentException("El evento de pago no trae clienteId/pedidoId");
                }
                notificacionService.notificarPago(evento);
            } else {
                throw new IllegalArgumentException("Routing key desconocida: " + routingKey);
            }
            channel.basicAck(tag, false);
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            rechazarADlq(e, tag, channel);
        } catch (Exception e) {
            reintentarUnaVez(e, message, tag, channel);
        }
    }

    private <T> T leer(Message message, Class<T> tipo) {
        try {
            return objectMapper.readValue(message.getBody(), tipo);
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON invalido: " + e.getMessage(), e);
        }
    }

    /** Error sin solucion reintentando: NACK sin requeue -> el DLX lo envia a la DLQ. */
    private void rechazarADlq(Exception e, long tag, Channel channel) throws IOException {
        log.error("[RABBIT][DLQ] Mensaje no recuperable en notificaciones.queue -> DLQ. Motivo: {}", e.getMessage());
        channel.basicNack(tag, false, false);
    }

    /** Error posiblemente transitorio: una vuelta a la cola; si vuelve a fallar, a la DLQ. */
    private void reintentarUnaVez(Exception e, Message message, long tag, Channel channel)
            throws IOException {
        if (Boolean.TRUE.equals(message.getMessageProperties().isRedelivered())) {
            log.error("[RABBIT][DLQ] Fallo repetido en notificaciones.queue -> DLQ. Motivo: {}", e.getMessage());
            channel.basicNack(tag, false, false);
        } else {
            log.warn("[RABBIT] Error recuperable en notificaciones.queue, se reintenta una vez: {}", e.getMessage());
            channel.basicNack(tag, false, true);
        }
    }
}
