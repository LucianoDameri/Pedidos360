package cl.duoc.pedidos360.ms_pedidos.messaging.event;

import cl.duoc.pedidos360.ms_pedidos.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Evento que ms-pedidos publica cuando un pedido se crea o se cancela. */
public record PedidoEvent(
        String tipo,
        Long pedidoId,
        Long clienteId,
        Long productoId,
        Integer cantidad,
        BigDecimal total,
        String estado,
        String ocurridoEn) {

    public static PedidoEvent desde(String tipo, Pedido pedido) {
        return new PedidoEvent(
                tipo,
                pedido.getId(),
                pedido.getClienteId(),
                pedido.getProductoId(),
                pedido.getCantidad(),
                pedido.getTotal(),
                pedido.getEstado() != null ? pedido.getEstado().name() : null,
                LocalDateTime.now().toString());
    }
}
