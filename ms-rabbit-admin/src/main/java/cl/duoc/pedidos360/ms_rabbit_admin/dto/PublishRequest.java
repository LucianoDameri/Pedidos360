package cl.duoc.pedidos360.ms_rabbit_admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Publicacion manual de un mensaje (sirve para probar colas y DLQ). */
@Data
public class PublishRequest {

    /** Exchange destino. Vacio = exchange por defecto (routingKey = nombre de la cola). */
    @NotNull(message = "El exchange es obligatorio (puede ir vacio para el exchange por defecto)")
    private String exchange;

    @NotBlank(message = "La routing key es obligatoria")
    private String routingKey;

    /** Cuerpo tal cual; se envia como application/json sin validarlo. */
    @NotBlank(message = "El mensaje (payload) es obligatorio")
    private String payload;
}
