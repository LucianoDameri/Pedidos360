package cl.duoc.pedidos360.ms_pagos.messaging.event;

import cl.duoc.pedidos360.ms_pagos.model.Pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Evento que ms-pagos publica cuando un pago es aprobado. */
public record PagoEvent(
        String tipo,
        Long pagoId,
        Long pedidoId,
        Long clienteId,
        BigDecimal monto,
        String metodoPago,
        String referencia,
        String ocurridoEn) {

    public static PagoEvent desde(String tipo, Pago pago, Long clienteId) {
        return new PagoEvent(
                tipo,
                pago.getId(),
                pago.getPedidoId(),
                clienteId,
                pago.getMonto(),
                pago.getMetodoPago() != null ? pago.getMetodoPago().name() : null,
                pago.getReferencia(),
                LocalDateTime.now().toString());
    }
}
