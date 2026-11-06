package cl.duoc.pedidos360.ms_pedidos.messaging.event;

import java.math.BigDecimal;

/** Evento publicado por ms-pagos cuando un pago es aprobado. */
public record PagoEvent(
        String tipo,
        Long pagoId,
        Long pedidoId,
        Long clienteId,
        BigDecimal monto,
        String metodoPago,
        String referencia,
        String ocurridoEn) {
}
