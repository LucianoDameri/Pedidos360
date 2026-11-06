package cl.duoc.pedidos360.ms_analitica.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo de errores de los consumidores Kafka (equivalente a la DLQ de RabbitMQ).
 *
 *  1. Un listener lanza una excepcion al procesar un mensaje.
 *  2. Si es NO recuperable (mensaje invalido, JSON roto) -> va directo a la DLT.
 *  3. Si es recuperable (base de datos caida...) -> se reintenta N veces con una pausa.
 *  4. Agotados los reintentos -> el mensaje original se publica en <topico>.DLT y se
 *     registra en el log con la etiqueta [KAFKA][DLT]. El consumidor sigue con el siguiente
 *     mensaje, asi un mensaje defectuoso nunca bloquea el procesamiento.
 *
 * Spring Boot aplica automaticamente el DefaultErrorHandler a los @KafkaListener.
 */
@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.dlt.suffix}")
    private String dltSuffix;

    @Value("${app.kafka.dlt.partitions}")
    private int dltPartitions;

    @Value("${app.kafka.dlt.replicas}")
    private int dltReplicas;

    @Value("${app.kafka.dlt.retention-ms}")
    private long dltRetentionMs;

    @Value("${app.kafka.topics.pedidos-eventos.name}")
    private String topicoPedidos;

    @Value("${app.kafka.topics.inventario-alertas.name}")
    private String topicoAlertas;

    // ------------------------------------------------------------------ topicos DLT
    @Bean
    public NewTopic pedidosEventosDlt() {
        return dlt(topicoPedidos + dltSuffix);
    }

    @Bean
    public NewTopic inventarioAlertasDlt() {
        return dlt(topicoAlertas + dltSuffix);
    }

    private NewTopic dlt(String nombre) {
        return TopicBuilder.name(nombre)
                .partitions(dltPartitions)
                .replicas(dltReplicas)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(dltRetentionMs))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    // ------------------------------------------------------------------ productores hacia la DLT
    /** Para mensajes que no se pudieron deserializar: se reenvian tal cual (bytes originales). */
    @Bean
    public KafkaTemplate<String, byte[]> dltBytesTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                propiedadesProductor("ms-analitica-dlt-bytes"), new StringSerializer(), new ByteArraySerializer()));
    }

    /** Para mensajes ya convertidos a DTO que fallaron en la logica de negocio. */
    @Bean
    public KafkaTemplate<String, Object> dltJsonTemplate() {
        JsonSerializer<Object> json = new JsonSerializer<>();
        json.setAddTypeInfo(false);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                propiedadesProductor("ms-analitica-dlt-json"), new StringSerializer(), json));
    }

    private Map<String, Object> propiedadesProductor(String clientId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        return props;
    }

    // ------------------------------------------------------------------ manejador de errores
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, byte[]> dltBytesTemplate,
                                                 KafkaTemplate<String, Object> dltJsonTemplate,
                                                 @Value("${app.kafka.retry.attempts}") long reintentos,
                                                 @Value("${app.kafka.retry.interval-ms}") long intervaloMs) {
        Map<Class<?>, KafkaOperations<?, ?>> plantillas = new LinkedHashMap<>();
        plantillas.put(byte[].class, dltBytesTemplate);   // primero: mensajes que no se pudieron leer
        plantillas.put(Object.class, dltJsonTemplate);    // el resto: DTO serializados como JSON

        // Destino: <topico original>.DLT. Particion -1 = la elige el productor (la DLT tiene 1)
        DeadLetterPublishingRecoverer publicador = new DeadLetterPublishingRecoverer(plantillas,
                (registro, error) -> new TopicPartition(registro.topic() + dltSuffix, -1));

        ConsumerRecordRecoverer recuperador = (registro, error) -> {
            log.error("[KAFKA][DLT] Mensaje enviado a {}{} (topico={}, particion={}, offset={}, clave={}). Motivo: {}",
                    registro.topic(), dltSuffix, registro.topic(), registro.partition(), registro.offset(),
                    registro.key(), NestedExceptionUtils.getMostSpecificCause(error).getMessage());
            publicador.accept(registro, null, error);
        };

        DefaultErrorHandler manejador = new DefaultErrorHandler(recuperador, new FixedBackOff(intervaloMs, reintentos));
        // Un mensaje invalido no mejora reintentando: directo a la DLT
        // (DeserializationException ya viene como no reintentable por defecto)
        manejador.addNotRetryableExceptions(IllegalArgumentException.class);
        // Tras enviarlo a la DLT se confirma su offset: no vuelve a leerse si el servicio se reinicia
        manejador.setCommitRecovered(true);
        manejador.setRetryListeners((registro, error, intento) ->
                log.warn("[KAFKA] Fallo al procesar (topico={}, particion={}, offset={}), intento {}: {}",
                        registro.topic(), registro.partition(), registro.offset(), intento,
                        NestedExceptionUtils.getMostSpecificCause(error).getMessage()));
        return manejador;
    }
}
