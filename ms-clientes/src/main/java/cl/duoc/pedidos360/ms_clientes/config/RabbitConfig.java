package cl.duoc.pedidos360.ms_clientes.config;

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
 * Topologia RabbitMQ de ms-clientes (caso de uso "notificaciones").
 *
 *   pedidos.exchange --(pedido.*)-------> notificaciones.pedido.queue --+
 *   pagos.exchange   --(pago.aprobado)--> notificaciones.pago.queue ----+--(rechazo)--> dlx.exchange --> notificaciones.dlq
 *
 * Los nombres vienen de application.yml (MessagingProperties).
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
    public Queue notificacionesPedidoQueue() {
        return conDeadLetter(props.getQueues().getNotificacionesPedido());
    }

    @Bean
    public Queue notificacionesPagoQueue() {
        return conDeadLetter(props.getQueues().getNotificacionesPago());
    }

    @Bean
    public Queue notificacionesDlq() {
        return QueueBuilder.durable(props.getQueues().getNotificacionesDlq()).build();
    }

    // ---------------------------------------------------------------- bindings
    @Bean
    public Binding notificacionesPedidoBinding() {
        return BindingBuilder.bind(notificacionesPedidoQueue())
                .to(pedidosExchange())
                .with(props.getRoutingKeys().getPedidoTodos());
    }

    @Bean
    public Binding notificacionesPagoBinding() {
        return BindingBuilder.bind(notificacionesPagoQueue())
                .to(pagosExchange())
                .with(props.getRoutingKeys().getPagoAprobado());
    }

    @Bean
    public Binding notificacionesDlqBinding() {
        return BindingBuilder.bind(notificacionesDlq())
                .to(dlxExchange())
                .with(props.getRoutingKeys().getNotificacionesDlq());
    }

    /** Cola durable cuyos mensajes rechazados se re-enrutan al DLX. */
    private Queue conDeadLetter(String nombre) {
        return QueueBuilder.durable(nombre)
                .withArgument("x-dead-letter-exchange", props.getExchanges().getDlx())
                .withArgument("x-dead-letter-routing-key", props.getRoutingKeys().getNotificacionesDlq())
                .build();
    }
}
