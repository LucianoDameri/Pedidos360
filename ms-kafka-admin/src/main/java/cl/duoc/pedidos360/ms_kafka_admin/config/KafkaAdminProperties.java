package cl.duoc.pedidos360.ms_kafka_admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Parametros del administrador (prefijo app.kafka-admin en application.yml). */
@Data
@Component
@ConfigurationProperties(prefix = "app.kafka-admin")
public class KafkaAdminProperties {

    /** Tiempo maximo (ms) de espera de cada operacion contra Kafka. */
    private long timeoutMs = 8000;

    /** Particiones por defecto al crear un topico. */
    private int defaultPartitions = 3;

    /** Factor de replicacion por defecto al crear un topico. */
    private short defaultReplicationFactor = 2;
}
