package cl.duoc.pedidos360.ms_pedidos.config;

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
    private Retention retention = new Retention();

    @Data
    public static class Exchanges {
        private String pedidos;
        private String pagos;
        private String dlx;
    }

    @Data
    public static class Queues {
        private String ticketsPago;
        private String ticketsDlq;
    }

    @Data
    public static class RoutingKeys {
        private String pedidoCreado;
        private String pedidoCancelado;
        private String pagoAprobado;
        private String ticketsDlq;
    }

    @Data
    public static class Retention {
        /** Tiempo (ms) que un mensaje permanece en una DLQ antes de expirar. */
        private long dlqTtlMs = 86400000L;
    }

}
