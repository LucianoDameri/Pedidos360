package cl.duoc.pedidos360.ms_clientes.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Nombres de exchanges, colas y routing keys de RabbitMQ.
 * Se leen desde application.yml (prefijo app.messaging): ningun nombre
 * esta escrito en el codigo de negocio.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.messaging")
public class MessagingProperties {

    private Exchanges exchanges = new Exchanges();
    private Queues queues = new Queues();
    private RoutingKeys routingKeys = new RoutingKeys();

    @Data
    public static class Exchanges {
        private String pedidos;
        private String pagos;
        private String dlx;
    }

    @Data
    public static class Queues {
        private String notificacionesPedido;
        private String notificacionesPago;
        private String notificacionesDlq;
    }

    @Data
    public static class RoutingKeys {
        private String pedidoTodos;
        private String pagoAprobado;
        private String notificacionesDlq;
    }

}
