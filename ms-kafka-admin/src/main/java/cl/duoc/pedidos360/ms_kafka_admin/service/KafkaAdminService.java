package cl.duoc.pedidos360.ms_kafka_admin.service;

import cl.duoc.pedidos360.ms_kafka_admin.config.KafkaAdminProperties;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.BrokerInfo;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.ClusterInfo;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.GroupDetail;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.GroupMember;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.GroupOffset;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.GroupSummary;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.PartitionInfo;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.PublishResult;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.TopicDetail;
import cl.duoc.pedidos360.ms_kafka_admin.dto.KafkaResponses.TopicSummary;
import cl.duoc.pedidos360.ms_kafka_admin.dto.PublishRequest;
import cl.duoc.pedidos360.ms_kafka_admin.dto.TopicConfigRequest;
import cl.duoc.pedidos360.ms_kafka_admin.dto.TopicRequest;
import cl.duoc.pedidos360.ms_kafka_admin.exception.ConflictoException;
import cl.duoc.pedidos360.ms_kafka_admin.exception.KafkaNoDisponibleException;
import cl.duoc.pedidos360.ms_kafka_admin.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Operaciones de administracion de Kafka sobre el AdminClient:
 * topicos (crear, listar, describir, cambiar configuracion, aumentar particiones, eliminar),
 * consumer groups (listar, describir con lag, eliminar), cluster y mensajes de prueba.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaAdminService {

    /** Configuraciones del topico que se muestran en la respuesta. */
    static final List<String> CONFIGS_VISIBLES = List.of(
            TopicConfig.RETENTION_MS_CONFIG,
            TopicConfig.RETENTION_BYTES_CONFIG,
            TopicConfig.CLEANUP_POLICY_CONFIG,
            TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG,
            TopicConfig.MAX_MESSAGE_BYTES_CONFIG,
            TopicConfig.SEGMENT_BYTES_CONFIG,
            TopicConfig.COMPRESSION_TYPE_CONFIG);

    private final Admin admin;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaAdminProperties props;

    // =========================================================================== cluster
    public ClusterInfo cluster() {
        DescribeClusterResult resultado = admin.describeCluster();
        String clusterId = esperar(resultado.clusterId());
        Node controlador = esperar(resultado.controller());
        List<BrokerInfo> brokers = esperar(resultado.nodes()).stream()
                .sorted(Comparator.comparingInt(Node::id))
                .map(n -> new BrokerInfo(n.id(), n.host(), n.port()))
                .toList();
        return new ClusterInfo(clusterId, controlador != null ? controlador.id() : null, brokers);
    }

    // =========================================================================== topicos
    public List<TopicSummary> listarTopicos(boolean incluirInternos) {
        Set<String> nombres = esperar(admin.listTopics(new ListTopicsOptions().listInternal(incluirInternos)).names());
        if (nombres.isEmpty()) {
            return List.of();
        }
        Map<String, TopicDescription> descripciones = esperar(admin.describeTopics(nombres).allTopicNames());
        return descripciones.values().stream()
                .sorted(Comparator.comparing(TopicDescription::name))
                .map(d -> new TopicSummary(d.name(), d.partitions().size(), factorReplicacion(d)))
                .toList();
    }

    public TopicDetail describirTopico(String nombre) {
        TopicDescription descripcion = esperar(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre);
        if (descripcion == null) {
            throw new RecursoNoEncontradoException("El topico '" + nombre + "' no existe");
        }
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, nombre);
        Config config = esperar(admin.describeConfigs(List.of(recurso)).all()).get(recurso);

        Map<String, String> configs = new LinkedHashMap<>();
        if (config != null) {
            for (String clave : CONFIGS_VISIBLES) {
                ConfigEntry entrada = config.get(clave);
                if (entrada != null) {
                    configs.put(clave, entrada.value());
                }
            }
        }
        List<PartitionInfo> particiones = descripcion.partitions().stream()
                .sorted(Comparator.comparingInt(TopicPartitionInfo::partition))
                .map(p -> new PartitionInfo(
                        p.partition(),
                        p.leader() != null ? p.leader().id() : null,
                        p.replicas().stream().map(Node::id).toList(),
                        p.isr().stream().map(Node::id).toList()))
                .toList();
        return new TopicDetail(descripcion.name(), descripcion.isInternal(), descripcion.partitions().size(),
                factorReplicacion(descripcion), particiones, configs);
    }

    public TopicDetail crearTopico(TopicRequest peticion) {
        validarNoProtegido(peticion.getName());
        int particiones = peticion.getPartitions() != null ? peticion.getPartitions() : props.getDefaultPartitions();
        short replicas = peticion.getReplicationFactor() != null
                ? peticion.getReplicationFactor() : props.getDefaultReplicationFactor();
        if (peticion.getMinInsyncReplicas() != null && peticion.getMinInsyncReplicas() > replicas) {
            throw new IllegalArgumentException(
                    "minInsyncReplicas (" + peticion.getMinInsyncReplicas() + ") no puede ser mayor que el factor de replicacion (" + replicas + ")");
        }

        NewTopic nuevo = new NewTopic(peticion.getName(), particiones, replicas)
                .configs(construirConfigs(peticion.getRetentionMs(), peticion.getCleanupPolicy(),
                        peticion.getMinInsyncReplicas(), peticion.getMaxMessageBytes()));
        esperar(admin.createTopics(List.of(nuevo)).all());
        log.info("[KAFKA-ADMIN] Topico creado: {} (particiones={}, replicas={})", peticion.getName(), particiones, replicas);
        return describirTopico(peticion.getName());
    }

    public TopicDetail actualizarConfiguracion(String nombre, TopicConfigRequest peticion) {
        Map<String, String> cambios = construirConfigs(peticion.getRetentionMs(), peticion.getCleanupPolicy(),
                peticion.getMinInsyncReplicas(), peticion.getMaxMessageBytes());
        if (cambios.isEmpty()) {
            throw new IllegalArgumentException(
                    "Indica al menos una configuracion: retentionMs, cleanupPolicy, minInsyncReplicas o maxMessageBytes");
        }
        TopicDetail actual = describirTopico(nombre);   // 404 si no existe
        if (peticion.getMinInsyncReplicas() != null && peticion.getMinInsyncReplicas() > actual.replicationFactor()) {
            throw new IllegalArgumentException("minInsyncReplicas (" + peticion.getMinInsyncReplicas()
                    + ") no puede ser mayor que el factor de replicacion del topico (" + actual.replicationFactor() + ")");
        }

        List<AlterConfigOp> operaciones = new ArrayList<>();
        cambios.forEach((clave, valor) ->
                operaciones.add(new AlterConfigOp(new ConfigEntry(clave, valor), AlterConfigOp.OpType.SET)));
        Map<ConfigResource, java.util.Collection<AlterConfigOp>> peticionAlter = new HashMap<>();
        peticionAlter.put(new ConfigResource(ConfigResource.Type.TOPIC, nombre), operaciones);
        esperar(admin.incrementalAlterConfigs(peticionAlter).all());
        log.info("[KAFKA-ADMIN] Configuracion de {} actualizada: {}", nombre, cambios);
        return describirTopico(nombre);
    }

    public TopicDetail aumentarParticiones(String nombre, int nuevoTotal) {
        TopicDetail actual = describirTopico(nombre);   // 404 si no existe
        if (nuevoTotal <= actual.partitions()) {
            throw new IllegalArgumentException("Kafka solo permite aumentar las particiones: el topico tiene "
                    + actual.partitions() + " y se pidieron " + nuevoTotal);
        }
        esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(nuevoTotal))).all());
        log.info("[KAFKA-ADMIN] Particiones de {}: {} -> {}", nombre, actual.partitions(), nuevoTotal);
        return describirTopico(nombre);
    }

    public void eliminarTopico(String nombre) {
        validarNoProtegido(nombre);
        describirTopico(nombre);   // 404 si no existe
        esperar(admin.deleteTopics(List.of(nombre)).all());
        log.warn("[KAFKA-ADMIN] Topico eliminado: {}", nombre);
    }

    // =========================================================================== consumer groups
    public List<GroupSummary> listarGrupos() {
        return esperar(admin.listConsumerGroups().all()).stream()
                .sorted(Comparator.comparing(ConsumerGroupListing::groupId))
                .map(g -> new GroupSummary(g.groupId(), g.state().map(Enum::name).orElse("UNKNOWN")))
                .toList();
    }

    public GroupDetail describirGrupo(String groupId) {
        ConsumerGroupDescription descripcion = esperar(admin.describeConsumerGroups(List.of(groupId)).all()).get(groupId);
        Map<TopicPartition, OffsetAndMetadata> confirmados =
                esperar(admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());

        boolean noExiste = descripcion == null
                || (descripcion.state().name().equals("DEAD") && confirmados.isEmpty());
        if (noExiste) {
            throw new RecursoNoEncontradoException("El consumer group '" + groupId + "' no existe");
        }

        List<GroupMember> miembros = descripcion.members().stream()
                .sorted(Comparator.comparing(MemberDescription::consumerId))
                .map(m -> new GroupMember(m.consumerId(), m.clientId(), m.host(),
                        m.assignment().topicPartitions().stream()
                                .map(tp -> tp.topic() + "-" + tp.partition()).sorted().toList()))
                .toList();

        // lag = ultimo offset del topico - ultimo offset confirmado por el grupo
        Map<TopicPartition, OffsetSpec> pedido = new HashMap<>();
        confirmados.keySet().forEach(tp -> pedido.put(tp, OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> finales =
                pedido.isEmpty() ? Map.of() : esperar(admin.listOffsets(pedido).all());

        List<GroupOffset> offsets = new ArrayList<>();
        long lagTotal = 0;
        for (Map.Entry<TopicPartition, OffsetAndMetadata> e : confirmados.entrySet()) {
            ListOffsetsResult.ListOffsetsResultInfo fin = finales.get(e.getKey());
            long actual = e.getValue().offset();
            long ultimo = fin != null ? fin.offset() : actual;
            long lag = Math.max(0, ultimo - actual);
            lagTotal += lag;
            offsets.add(new GroupOffset(e.getKey().topic(), e.getKey().partition(), actual, ultimo, lag));
        }
        offsets.sort(Comparator.comparing(GroupOffset::topic).thenComparingInt(GroupOffset::partition));

        return new GroupDetail(groupId, descripcion.state().name(), descripcion.partitionAssignor(),
                miembros, offsets, lagTotal);
    }

    public void eliminarGrupo(String groupId) {
        esperar(admin.deleteConsumerGroups(List.of(groupId)).all());
        log.warn("[KAFKA-ADMIN] Consumer group eliminado: {}", groupId);
    }

    // =========================================================================== mensajes de prueba
    /** Publica un mensaje tal cual en un topico existente (sirve para probar consumidores y la DLT). */
    public PublishResult publicar(PublishRequest peticion) {
        Set<String> existentes = esperar(admin.listTopics(new ListTopicsOptions().listInternal(false)).names());
        if (!existentes.contains(peticion.getTopic())) {
            throw new RecursoNoEncontradoException("El topico '" + peticion.getTopic() + "' no existe");
        }
        try {
            SendResult<String, String> resultado = kafkaTemplate
                    .send(peticion.getTopic(), peticion.getKey(), peticion.getPayload())
                    .get(props.getTimeoutMs(), TimeUnit.MILLISECONDS);
            return new PublishResult(peticion.getTopic(),
                    resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("La publicacion fue interrumpida");
        } catch (TimeoutException e) {
            throw new KafkaNoDisponibleException("Kafka no confirmo el mensaje a tiempo");
        } catch (ExecutionException e) {
            throw traducir(e.getCause());
        }
    }

    // =========================================================================== utilidades
    private static int factorReplicacion(TopicDescription descripcion) {
        return descripcion.partitions().isEmpty() ? 0 : descripcion.partitions().get(0).replicas().size();
    }

    /** Arma el mapa de configuraciones de Kafka con lo que haya venido informado. */
    static Map<String, String> construirConfigs(Long retentionMs, String cleanupPolicy,
                                                Integer minInsyncReplicas, Integer maxMessageBytes) {
        Map<String, String> configs = new LinkedHashMap<>();
        if (retentionMs != null) {
            configs.put(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionMs));
        }
        if (cleanupPolicy != null) {
            configs.put(TopicConfig.CLEANUP_POLICY_CONFIG, cleanupPolicy);
        }
        if (minInsyncReplicas != null) {
            configs.put(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(minInsyncReplicas));
        }
        if (maxMessageBytes != null) {
            configs.put(TopicConfig.MAX_MESSAGE_BYTES_CONFIG, String.valueOf(maxMessageBytes));
        }
        return configs;
    }

    /** Los topicos internos de Kafka (__consumer_offsets, etc.) no se pueden crear ni eliminar desde aqui. */
    private static void validarNoProtegido(String nombre) {
        if (nombre.startsWith("__") || nombre.startsWith("_confluent")) {
            throw new IllegalArgumentException("El topico '" + nombre + "' es interno de Kafka y esta protegido");
        }
    }

    private <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(props.getTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaNoDisponibleException("La operacion fue interrumpida");
        } catch (TimeoutException e) {
            throw new KafkaNoDisponibleException("Kafka no respondio a tiempo: revisa que los brokers esten arriba");
        } catch (ExecutionException e) {
            throw traducir(e.getCause());
        }
    }

    /** Convierte los errores de Kafka en excepciones propias (el ApiExceptionHandler las vuelve 404/409/400/503). */
    static RuntimeException traducir(Throwable causa) {
        if (causa instanceof UnknownTopicOrPartitionException) {
            return new RecursoNoEncontradoException("El topico no existe");
        }
        if (causa instanceof GroupIdNotFoundException) {
            return new RecursoNoEncontradoException("El consumer group no existe");
        }
        if (causa instanceof TopicExistsException) {
            return new ConflictoException("El topico ya existe");
        }
        if (causa instanceof GroupNotEmptyException) {
            return new ConflictoException("El consumer group tiene miembros activos: detén los consumidores antes de eliminarlo");
        }
        if (causa instanceof InvalidReplicationFactorException) {
            return new IllegalArgumentException(
                    "El factor de replicacion pedido es mayor que los brokers disponibles: " + causa.getMessage());
        }
        if (causa instanceof InvalidTopicException || causa instanceof InvalidConfigurationException
                || causa instanceof InvalidPartitionsException || causa instanceof InvalidRequestException) {
            return new IllegalArgumentException(causa.getMessage());
        }
        if (causa instanceof org.apache.kafka.common.errors.TimeoutException) {
            return new KafkaNoDisponibleException("Kafka no respondio a tiempo: " + causa.getMessage());
        }
        if (causa instanceof RuntimeException runtime) {
            return runtime;
        }
        return new IllegalStateException("Error al hablar con Kafka: " + (causa != null ? causa.getMessage() : "desconocido"), causa);
    }
}
