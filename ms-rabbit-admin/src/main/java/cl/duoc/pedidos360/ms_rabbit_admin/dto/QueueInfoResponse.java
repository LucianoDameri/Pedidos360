package cl.duoc.pedidos360.ms_rabbit_admin.dto;

/** Estado de una cola. */
public record QueueInfoResponse(String name, int messageCount, int consumerCount) {
}
