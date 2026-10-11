package cl.duoc.pedidos360.ms_rabbit_admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Datos para crear o eliminar un binding cola <- exchange. */
@Data
public class BindingRequest {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    private String queue;

    @NotBlank(message = "El nombre del exchange es obligatorio")
    private String exchange;

    /** Puede ir vacia (por ejemplo en exchanges fanout). */
    private String routingKey = "";
}
