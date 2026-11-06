package cl.duoc.pedidos360.ms_inventario.messaging.event;

import java.math.BigDecimal;

/** Evento publicado por ms-pedidos cuando un pedido se crea o se cancela. */
public record PedidoEvent(
        String tipo,
        Long pedidoId,
        Long clienteId,
        Long productoId,
        Integer cantidad,
        BigDecimal total,
        String estado,
        String ocurridoEn) {
}
