package cl.duoc.pedidos360.ms_rabbit_admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Datos para crear una cola. */
@Data
public class QueueRequest {

    @NotBlank(message = "El nombre de la cola es obligatorio")
    @Pattern(regexp = "^(?!amq\\.)[A-Za-z0-9._-]{1,255}$",
            message = "Nombre de cola invalido: solo letras, numeros, '.', '_' o '-' (maximo 255) y no puede empezar con 'amq.'")
    private String name;

    private boolean durable = true;

    /** Exchange al que se envian los mensajes muertos (DLX). Opcional. */
    private String deadLetterExchange;

    /** Routing key con la que se re-enruta el mensaje muerto. Requiere deadLetterExchange. */
    private String deadLetterRoutingKey;

    @Min(value = 1, message = "El TTL de mensajes debe ser mayor a 0 ms")
    private Integer messageTtlMs;

    @Min(value = 1, message = "El largo maximo de la cola debe ser mayor a 0")
    private Integer maxLength;
}
