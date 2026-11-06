package cl.duoc.pedidos360.ms_kafka_admin.dto;

import java.util.List;
import java.util.Map;

/** Mensajes de SALIDA de la API de administracion (se serializan como JSON). */
public final class KafkaResponses {

    private KafkaResponses() {
    }

    public record BrokerInfo(int id, String host, int port) {
    }

    public record ClusterInfo(String clusterId, Integer controller, List<BrokerInfo> brokers) {
    }

    public record TopicSummary(String name, int partitions, int replicationFactor) {
    }

    public record PartitionInfo(int partition, Integer leader, List<Integer> replicas, List<Integer> isr) {
    }

    public record TopicDetail(String name, boolean internal, int partitions, int replicationFactor,
                              List<PartitionInfo> partitionInfo, Map<String, String> configs) {
    }

    public record GroupSummary(String groupId, String state) {
    }

    public record GroupMember(String memberId, String clientId, String host, List<String> partitions) {
    }

    public record GroupOffset(String topic, int partition, long currentOffset, long endOffset, long lag) {
    }

    public record GroupDetail(String groupId, String state, String partitionAssignor,
                              List<GroupMember> members, List<GroupOffset> offsets, long totalLag) {
    }

    public record PublishResult(String topic, int partition, long offset) {
    }
}
