package cl.duoc.pedidos360.ms_rabbit_admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Datos para crear un exchange. */
@Data
public class ExchangeRequest {

    @NotBlank(message = "El nombre del exchange es obligatorio")
    @Pattern(regexp = "^(?!amq\\.)[A-Za-z0-9._-]{1,255}$",
            message = "Nombre de exchange invalido: solo letras, numeros, '.', '_' o '-' (maximo 255) y no puede empezar con 'amq.'")
    private String name;

    @NotNull(message = "El tipo de exchange es obligatorio (DIRECT, TOPIC, FANOUT o HEADERS)")
    private ExchangeKind type;

    private boolean durable = true;
}
