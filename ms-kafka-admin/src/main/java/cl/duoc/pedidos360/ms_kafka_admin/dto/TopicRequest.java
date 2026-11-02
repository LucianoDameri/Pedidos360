package cl.duoc.pedidos360.ms_kafka_admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Datos para crear un topico. Lo que no se indique toma el valor por defecto del servicio o del broker. */
@Data
public class TopicRequest {

    @NotBlank(message = "El nombre del topico es obligatorio")
    @Pattern(regexp = "^(?!\\.{1,2}$)[A-Za-z0-9._-]{1,249}$",
            message = "Nombre de topico invalido: solo letras, numeros, '.', '_' o '-' (maximo 249)")
    private String name;

    @Min(value = 1, message = "Las particiones deben ser al menos 1")
    private Integer partitions;

    @Min(value = 1, message = "El factor de replicacion debe ser al menos 1")
    private Short replicationFactor;

    /** Retencion en milisegundos (-1 = sin limite). */
    @Min(value = -1, message = "retentionMs debe ser -1 (sin limite) o mayor a 0")
    private Long retentionMs;

    @Pattern(regexp = "^(delete|compact|compact,delete)$",
            message = "cleanupPolicy debe ser delete, compact o compact,delete")
    private String cleanupPolicy;

    @Min(value = 1, message = "minInsyncReplicas debe ser al menos 1")
    private Integer minInsyncReplicas;

    @Min(value = 1, message = "maxMessageBytes debe ser mayor a 0")
    private Integer maxMessageBytes;
}
