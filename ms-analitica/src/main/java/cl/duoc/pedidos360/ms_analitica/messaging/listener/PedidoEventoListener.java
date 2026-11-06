package cl.duoc.pedidos360.ms_analitica.messaging.listener;

import cl.duoc.pedidos360.ms_analitica.messaging.dto.PedidoEventoDto;
import cl.duoc.pedidos360.ms_analitica.service.VentasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumidor del topico "pedidos.eventos" -> metricas de venta.
 *
 * Consumer group "analitica-ventas": sus 3 hilos se reparten las 3 particiones del topico y,
 * si se levanta otra copia del servicio, Kafka vuelve a repartirlas entre todos los miembros.
 *
 * Aqui solo hay mensajeria: la logica de negocio esta en VentasService. Si el servicio lanza
 * una excepcion NO se confirma el offset y el manejador de errores (KafkaConsumerConfig)
 * decide: reintentar o enviar el mensaje a la DLT.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PedidoEventoListener {

    private final VentasService ventasService;

    @KafkaListener(
            topics = "${app.kafka.topics.pedidos-eventos.name}",
            groupId = "${app.kafka.groups.ventas}",
            concurrency = "${app.kafka.concurrency.ventas}",
            properties = "spring.json.value.default.type=cl.duoc.pedidos360.ms_analitica.messaging.dto.PedidoEventoDto")
    public void onPedidoEvento(ConsumerRecord<String, PedidoEventoDto> registro, Acknowledgment ack) {
        log.debug("[KAFKA] Recibido de {} particion={} offset={} clave={}",
                registro.topic(), registro.partition(), registro.offset(), registro.key());
        ventasService.registrar(registro.value());
        ack.acknowledge();
    }
}
