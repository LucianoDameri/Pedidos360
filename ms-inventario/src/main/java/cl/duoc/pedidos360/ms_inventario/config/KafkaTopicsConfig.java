package cl.duoc.pedidos360.ms_inventario.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declara los topicos que este servicio produce. Spring (KafkaAdmin) los crea al arrancar
 * si no existen; si ya existen no los modifica (los cambios se hacen con ms-kafka-admin).
 *
 * inventario.alertas usa cleanup.policy = compact,delete: como la clave es el productoId,
 * Kafka conserva al menos la ultima alerta de cada producto y borra lo anterior a 30 dias.
 */
@Configuration
@RequiredArgsConstructor
public class KafkaTopicsConfig {

    private final KafkaTopicsProperties props;

    @Bean
    public NewTopic inventarioAlertasTopic() {
        KafkaTopicsProperties.TopicSpec t = props.getTopics().getInventarioAlertas();
        return TopicBuilder.name(t.getName())
                .partitions(t.getPartitions())
                .replicas(t.getReplicas())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(t.getRetentionMs()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, t.getCleanupPolicy())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(t.getMinInsyncReplicas()))
                .build();
    }
}
