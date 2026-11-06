package cl.duoc.pedidos360.ms_inventario.messaging.listener;

import cl.duoc.pedidos360.ms_inventario.messaging.event.PedidoEvent;
import cl.duoc.pedidos360.ms_inventario.service.AlertaStockService;
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
 * Consumidor del dominio "inventario" (pedido creado/cancelado -> alerta de stock bajo).
 *
 * ACK manual y manejo de errores:
 *  - OK                              -> basicAck.
 *  - Error NO recuperable (JSON roto, datos invalidos, producto sin stock registrado)
 *                                    -> basicNack(requeue=false): va directo a la DLQ.
 *  - Error recuperable (BD caida...) -> un reintento (basicNack requeue=true);
 *                                       si falla de nuevo, basicNack(requeue=false) -> DLQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockListener {

    private final ObjectMapper objectMapper;
    private final AlertaStockService alertaStockService;

    @RabbitListener(queues = "${app.messaging.queues.inventario-stock}")
    public void onPedido(Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            PedidoEvent evento = leer(message);
            if (evento == null || evento.productoId() == null) {
                throw new IllegalArgumentException("El evento no trae productoId");
            }
            alertaStockService.evaluar(evento);
            channel.basicAck(tag, false);
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            log.error("[RABBIT][DLQ] Mensaje no recuperable en inventario.stock.queue -> DLQ. Motivo: {}", e.getMessage());
            channel.basicNack(tag, false, false);
        } catch (Exception e) {
            if (Boolean.TRUE.equals(message.getMessageProperties().isRedelivered())) {
                log.error("[RABBIT][DLQ] Fallo repetido en inventario.stock.queue -> DLQ. Motivo: {}", e.getMessage());
                channel.basicNack(tag, false, false);
            } else {
                log.warn("[RABBIT] Error recuperable en inventario.stock.queue, se reintenta una vez: {}", e.getMessage());
                channel.basicNack(tag, false, true);
            }
        }
    }

    private PedidoEvent leer(Message message) {
        try {
            return objectMapper.readValue(message.getBody(), PedidoEvent.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("JSON invalido: " + e.getMessage(), e);
        }
    }
}
