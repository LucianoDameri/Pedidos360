package cl.duoc.pedidos360.ms_inventario.messaging.publisher;

import cl.duoc.pedidos360.ms_inventario.config.KafkaTopicsProperties;
import cl.duoc.pedidos360.ms_inventario.messaging.event.AlertaStockEventoDto;
import cl.duoc.pedidos360.ms_inventario.model.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Productor Kafka del topico "inventario.alertas".
 *
 *  - La clave es el productoId: las alertas de un mismo producto siempre van a la misma particion.
 *  - Si Kafka no responde solo se registra el error: el ajuste de stock ya quedo guardado y
 *    la operacion del cliente no debe fallar por la mensajeria.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertaStockKafkaPublisher {

    private final KafkaTemplate<String, AlertaStockEventoDto> kafkaTemplate;
    private final KafkaTopicsProperties props;

    public void publicar(Stock stock) {
        AlertaStockEventoDto evento = AlertaStockEventoDto.desde(stock);
        String topico = props.getTopics().getInventarioAlertas().getName();
        String clave = String.valueOf(stock.getProductoId());
        try {
            kafkaTemplate.send(topico, clave, evento).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("[KAFKA] No se pudo publicar {} (productoId={}) en {}: {}",
                            evento.tipo(), evento.productoId(), topico, error.getMessage());
                } else {
                    log.warn("[KAFKA] Publicada alerta {} (productoId={}, cantidad={}, minimo={}) en {} particion={} offset={}",
                            evento.tipo(), evento.productoId(), evento.cantidadActual(), evento.stockMinimo(), topico,
                            resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset());
                }
            });
        } catch (RuntimeException e) {
            log.error("[KAFKA] No se pudo publicar {} (productoId={}) en {}: {}",
                    evento.tipo(), evento.productoId(), topico, e.getMessage());
        }
    }
}
