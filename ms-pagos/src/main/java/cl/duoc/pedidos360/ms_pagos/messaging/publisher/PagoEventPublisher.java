package cl.duoc.pedidos360.ms_pagos.messaging.publisher;

import cl.duoc.pedidos360.ms_pagos.config.MessagingProperties;
import cl.duoc.pedidos360.ms_pagos.messaging.event.PagoEvent;
import cl.duoc.pedidos360.ms_pagos.model.Pago;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Productor de eventos de pago. Si RabbitMQ no esta disponible solo se
 * registra el error: el pago ya quedo aprobado y no se revierte por eso.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PagoEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties props;

    public void pagoAprobado(Pago pago, Long clienteId) {
        PagoEvent evento = PagoEvent.desde("PAGO_APROBADO", pago, clienteId);
        String routingKey = props.getRoutingKeys().getPagoAprobado();
        try {
            rabbitTemplate.convertAndSend(props.getExchanges().getPagos(), routingKey, evento);
            log.info("[RABBIT] Publicado PAGO_APROBADO (pedidoId={}) en {} con routing key {}",
                    evento.pedidoId(), props.getExchanges().getPagos(), routingKey);
        } catch (AmqpException e) {
            log.error("[RABBIT] No se pudo publicar PAGO_APROBADO (pedidoId={}). El pago sigue aprobado: {}",
                    evento.pedidoId(), e.getMessage());
        }
    }
}
