package cl.duoc.pedidos360.ms_kafka_admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Configuracion de un topico existente. Solo se modifican los campos que vengan en el JSON. */
@Data
public class TopicConfigRequest {

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
