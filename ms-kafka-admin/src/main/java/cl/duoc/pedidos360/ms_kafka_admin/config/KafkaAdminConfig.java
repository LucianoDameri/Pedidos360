package cl.duoc.pedidos360.ms_kafka_admin.config;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/** Cliente administrador de Kafka (AdminClient): crea, consulta, modifica y elimina recursos. */
@Configuration
public class KafkaAdminConfig {

    @Bean(destroyMethod = "close")
    public Admin kafkaAdminClient(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
                                  KafkaAdminProperties props) {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(AdminClientConfig.CLIENT_ID_CONFIG, "ms-kafka-admin-client");
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) props.getTimeoutMs());
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, (int) props.getTimeoutMs());
        return Admin.create(config);
    }
}
