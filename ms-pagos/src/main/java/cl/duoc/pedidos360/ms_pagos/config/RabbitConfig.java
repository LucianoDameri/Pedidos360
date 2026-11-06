package cl.duoc.pedidos360.ms_pagos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ de ms-pagos. Solo es productor: declara pagos.exchange,
 * por donde publica pago.aprobado. Las colas las declaran los consumidores
 * (ms-pedidos para tickets, ms-clientes para notificaciones).
 */
@Configuration
@RequiredArgsConstructor
public class RabbitConfig {

    private final MessagingProperties props;

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public TopicExchange pagosExchange() {
        return new TopicExchange(props.getExchanges().getPagos(), true, false);
    }
}
