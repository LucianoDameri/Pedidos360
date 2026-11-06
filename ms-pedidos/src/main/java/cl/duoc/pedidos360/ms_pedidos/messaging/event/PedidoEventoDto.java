package cl.duoc.pedidos360.ms_pedidos.messaging.event;

import cl.duoc.pedidos360.ms_pedidos.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mensaje de SALIDA del topico Kafka "pedidos.eventos" (JSON). Clave del mensaje: pedidoId.
 *
 * tipo: PEDIDO_CREADO | PEDIDO_CONFIRMADO | PEDIDO_EN_PREPARACION | PEDIDO_EN_CAMINO
 *       | PEDIDO_ENTREGADO | PEDIDO_CANCELADO
 *
 * Ejemplo:
 * {"eventId":"6f1c...","tipo":"PEDIDO_CONFIRMADO","version":1,"pedidoId":12,"clienteId":3,
 *  "productoId":7,"cantidad":2,"total":159980,"estado":"CONFIRMADO","ocurridoEn":"2026-11-02T14:35:10"}
 *
 * El contrato JSON esta documentado en docs/KAFKA.md. Los consumidores tienen su propio DTO
 * (no comparten clases), por eso el productor NO envia cabeceras de tipo Java.
 */
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

    public static final int VERSION = 1;

    public static PedidoEventoDto desde(String tipo, Pedido pedido) {
        return new PedidoEventoDto(
                UUID.randomUUID().toString(),
                tipo,
                VERSION,
                pedido.getId(),
                pedido.getClienteId(),
                pedido.getProductoId(),
                pedido.getCantidad(),
                pedido.getTotal(),
                pedido.getEstado() != null ? pedido.getEstado().name() : null,
                LocalDateTime.now().toString());
    }
}
