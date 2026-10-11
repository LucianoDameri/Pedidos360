package cl.duoc.pedidos360.ms_pedidos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ de ms-pedidos. Aqui (y solo aqui) se declaran los beans
 * de Exchange, Queue y Binding; los nombres vienen de application.yml.
 *
 * Caso de uso "tickets": cuando ms-pagos aprueba un pago, ms-pedidos genera
 * el ticket del pedido de forma asincrona.
 *
 *   pagos.exchange --(pago.aprobado)--> tickets.pago.queue --(rechazo)--> dlx.exchange --> tickets.dlq
 *
 * Ademas declara pedidos.exchange, por donde ms-pedidos publica pedido.creado
 * y pedido.cancelado.
 */
@Configuration
@RequiredArgsConstructor
public class RabbitConfig {

    private final MessagingProperties props;

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // ---------------------------------------------------------------- exchanges
    @Bean
    public TopicExchange pedidosExchange() {
        return new TopicExchange(props.getExchanges().getPedidos(), true, false);
    }

    @Bean
    public TopicExchange pagosExchange() {
        return new TopicExchange(props.getExchanges().getPagos(), true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(props.getExchanges().getDlx(), true, false);
    }

    // ---------------------------------------------------------------- colas
    @Bean
    public Queue ticketsPagoQueue() {
        return QueueBuilder.durable(props.getQueues().getTicketsPago())
                .withArgument("x-dead-letter-exchange", props.getExchanges().getDlx())
                .withArgument("x-dead-letter-routing-key", props.getRoutingKeys().getTicketsDlq())
                .build();
    }

    @Bean
    public Queue ticketsDlq() {
        return QueueBuilder.durable(props.getQueues().getTicketsDlq()).build();
    }

    // ---------------------------------------------------------------- bindings
    @Bean
    public Binding ticketsPagoBinding() {
        return BindingBuilder.bind(ticketsPagoQueue())
                .to(pagosExchange())
                .with(props.getRoutingKeys().getPagoAprobado());
    }

    @Bean
    public Binding ticketsDlqBinding() {
        return BindingBuilder.bind(ticketsDlq())
                .to(dlxExchange())
                .with(props.getRoutingKeys().getTicketsDlq());
    }
}
