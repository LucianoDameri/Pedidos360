package cl.duoc.pedidos360.ms_inventario.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Parametros de los topicos Kafka que usa este microservicio (prefijo app.kafka en application.yml).
 * Ningun nombre ni parametro de topico esta escrito en el codigo de negocio.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaTopicsProperties {

    private Topics topics = new Topics();

    @Data
    public static class Topics {
        private TopicSpec inventarioAlertas = new TopicSpec();
    }

    @Data
    public static class TopicSpec {
        /** Nombre del topico. */
        private String name;
        /** Particiones: definen el paralelismo maximo de los consumidores de un grupo. */
        private int partitions = 2;
        /** Factor de replicacion (copias de cada particion en distintos brokers). */
        private int replicas = 3;
        /** Tiempo que Kafka conserva los mensajes (ms). */
        private long retentionMs = 2592000000L;
        /** delete (borra por antiguedad), compact (deja el ultimo valor por clave) o ambos. */
        private String cleanupPolicy = "compact,delete";
        /** Replicas que deben confirmar una escritura con acks=all. */
        private int minInsyncReplicas = 2;
    }
}
