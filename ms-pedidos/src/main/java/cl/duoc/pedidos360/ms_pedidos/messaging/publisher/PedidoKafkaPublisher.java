package cl.duoc.pedidos360.ms_pedidos.messaging.publisher;

import cl.duoc.pedidos360.ms_pedidos.config.KafkaTopicsProperties;
import cl.duoc.pedidos360.ms_pedidos.messaging.event.PedidoEventoDto;
import cl.duoc.pedidos360.ms_pedidos.model.Pedido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Productor Kafka del topico "pedidos.eventos".
 *
 *  - La clave es el pedidoId: todos los eventos de un mismo pedido caen en la misma particion
 *    y por lo tanto se leen en el orden en que ocurrieron.
 *  - El envio es asincrono. Si Kafka no responde solo se registra el error: la compra del
 *    cliente no debe fallar por la mensajeria (igual que con RabbitMQ).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PedidoKafkaPublisher {

    private final KafkaTemplate<String, PedidoEventoDto> kafkaTemplate;
    private final KafkaTopicsProperties props;

    public void publicar(String tipo, Pedido pedido) {
        PedidoEventoDto evento = PedidoEventoDto.desde(tipo, pedido);
        String topico = props.getTopics().getPedidosEventos().getName();
        String clave = String.valueOf(pedido.getId());
        try {
            kafkaTemplate.send(topico, clave, evento).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("[KAFKA] No se pudo publicar {} (pedidoId={}) en {}. El pedido sigue valido: {}",
                            evento.tipo(), evento.pedidoId(), topico, error.getMessage());
                } else {
                    log.info("[KAFKA] Publicado {} (pedidoId={}) en {} particion={} offset={}",
                            evento.tipo(), evento.pedidoId(), topico,
                            resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset());
                }
            });
        } catch (RuntimeException e) {
            log.error("[KAFKA] No se pudo publicar {} (pedidoId={}) en {}. El pedido sigue valido: {}",
                    evento.tipo(), evento.pedidoId(), topico, e.getMessage());
        }
    }
}
