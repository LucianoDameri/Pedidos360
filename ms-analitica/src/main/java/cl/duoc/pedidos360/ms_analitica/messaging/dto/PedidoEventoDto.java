package cl.duoc.pedidos360.ms_analitica.messaging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Mensaje de ENTRADA del topico "pedidos.eventos" (lo publica ms-pedidos). Clave: pedidoId.
 *
 * tipo: PEDIDO_CREADO | PEDIDO_CONFIRMADO | PEDIDO_EN_PREPARACION | PEDIDO_EN_CAMINO
 *       | PEDIDO_ENTREGADO | PEDIDO_CANCELADO
 *
 * Los campos desconocidos se ignoran: el productor puede agregar campos sin romper este consumidor.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoEventoDto(
        String eventId,
        String tipo,
        int version,
        Long pedidoId,
        Long clienteId,
        Long productoId,
        Integer cantidad,
        BigDecimal total,
        String estado,
        String ocurridoEn) {

    public static final String CREADO = "PEDIDO_CREADO";
    public static final String CONFIRMADO = "PEDIDO_CONFIRMADO";
    public static final String CANCELADO = "PEDIDO_CANCELADO";
}
