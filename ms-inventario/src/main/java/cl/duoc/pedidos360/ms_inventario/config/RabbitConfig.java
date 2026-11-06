package cl.duoc.pedidos360.ms_inventario.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ de ms-inventario (caso de uso "alertas de stock").
 *
 *   pedidos.exchange --(pedido.*)--> inventario.stock.queue --(rechazo)--> dlx.exchange --> inventario.dlq
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

    /**
     * Fabrica de los @RabbitListener. Los consumidores reciben el mensaje crudo (Message) y leen el JSON
     * con su propio ObjectMapper, porque Jackson2JsonMessageConverter intentaria convertirlo a la clase Java
     * del PRODUCTOR (cabecera __TypeId__), que no existe en este microservicio. Se conservan los ajustes
     * del application.yml (ACK manual, prefetch, requeue).
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(new SimpleMessageConverter());
        return factory;
    }

    // ---------------------------------------------------------------- exchanges
    @Bean
    public TopicExchange pedidosExchange() {
        return new TopicExchange(props.getExchanges().getPedidos(), true, false);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(props.getExchanges().getDlx(), true, false);
    }

    // ---------------------------------------------------------------- colas
    @Bean
    public Queue inventarioStockQueue() {
        return QueueBuilder.durable(props.getQueues().getInventarioStock())
                .withArgument("x-dead-letter-exchange", props.getExchanges().getDlx())
                .withArgument("x-dead-letter-routing-key", props.getRoutingKeys().getInventarioDlq())
                .build();
    }

    @Bean
    public Queue inventarioDlq() {
        return QueueBuilder.durable(props.getQueues().getInventarioDlq())
                .withArgument("x-message-ttl", props.getRetention().getDlqTtlMs())
                .build();
    }

    // ---------------------------------------------------------------- bindings
    @Bean
    public Binding inventarioStockBinding() {
        return BindingBuilder.bind(inventarioStockQueue())
                .to(pedidosExchange())
                .with(props.getRoutingKeys().getPedidoTodos());
    }

    @Bean
    public Binding inventarioDlqBinding() {
        return BindingBuilder.bind(inventarioDlq())
                .to(dlxExchange())
                .with(props.getRoutingKeys().getInventarioDlq());
    }
}
