package cl.duoc.pedidos360.ms_kafka_admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Mensaje de prueba (texto tal cual, normalmente un JSON) para publicar en un topico. */
@Data
public class PublishRequest {

    @NotBlank(message = "El topico es obligatorio")
    private String topic;

    /** Clave del mensaje (opcional): define la particion de destino. */
    private String key;

    @NotBlank(message = "El contenido del mensaje (payload) es obligatorio")
    private String payload;
}
