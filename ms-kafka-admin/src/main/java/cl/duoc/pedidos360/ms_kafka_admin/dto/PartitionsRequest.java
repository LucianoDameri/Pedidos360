package cl.duoc.pedidos360.ms_kafka_admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Nuevo numero total de particiones de un topico (Kafka solo permite aumentarlas). */
@Data
public class PartitionsRequest {

    @NotNull(message = "El numero de particiones es obligatorio")
    @Min(value = 1, message = "Las particiones deben ser al menos 1")
    private Integer partitions;
}
