package cl.duoc.pedidos360.ms_analitica.messaging.listener;

import cl.duoc.pedidos360.ms_analitica.messaging.dto.AlertaStockEventoDto;
import cl.duoc.pedidos360.ms_analitica.service.AlertasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumidor del topico "inventario.alertas" -> historial de reposicion.
 *
 * Consumer group "analitica-reposicion": es un grupo DISTINTO al de ventas, por eso cada
 * topico se procesa de forma independiente (cada grupo lleva sus propios offsets).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertaStockListener {

    private final AlertasService alertasService;

    @KafkaListener(
            topics = "${app.kafka.topics.inventario-alertas.name}",
            groupId = "${app.kafka.groups.reposicion}",
            concurrency = "${app.kafka.concurrency.reposicion}",
            properties = "spring.json.value.default.type=cl.duoc.pedidos360.ms_analitica.messaging.dto.AlertaStockEventoDto")
    public void onAlertaStock(ConsumerRecord<String, AlertaStockEventoDto> registro, Acknowledgment ack) {
        log.debug("[KAFKA] Recibido de {} particion={} offset={} clave={}",
                registro.topic(), registro.partition(), registro.offset(), registro.key());
        alertasService.registrar(registro.value());
        ack.acknowledge();
    }
}
