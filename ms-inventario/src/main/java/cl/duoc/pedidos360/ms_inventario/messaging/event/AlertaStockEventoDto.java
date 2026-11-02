package cl.duoc.pedidos360.ms_inventario.messaging.event;

import cl.duoc.pedidos360.ms_inventario.model.Stock;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mensaje de SALIDA del topico Kafka "inventario.alertas" (JSON). Clave del mensaje: productoId.
 *
 * tipo: STOCK_BAJO (cantidad <= stockMinimo) | STOCK_AGOTADO (cantidad = 0)
 *
 * Ejemplo:
 * {"eventId":"a41d...","tipo":"STOCK_BAJO","version":1,"productoId":7,"cantidadActual":3,
 *  "stockMinimo":5,"ocurridoEn":"2026-11-02T14:36:02"}
 *
 * El contrato JSON esta documentado en docs/KAFKA.md.
 */
public record AlertaStockEventoDto(
        String eventId,
        String tipo,
        int version,
        Long productoId,
        Integer cantidadActual,
        Integer stockMinimo,
        String ocurridoEn) {

    public static final int VERSION = 1;
    public static final String STOCK_BAJO = "STOCK_BAJO";
    public static final String STOCK_AGOTADO = "STOCK_AGOTADO";

    public static AlertaStockEventoDto desde(Stock stock) {
        String tipo = stock.getCantidad() != null && stock.getCantidad() == 0 ? STOCK_AGOTADO : STOCK_BAJO;
        return new AlertaStockEventoDto(
                UUID.randomUUID().toString(),
                tipo,
                VERSION,
                stock.getProductoId(),
                stock.getCantidad(),
                stock.getStockMinimo(),
                LocalDateTime.now().toString());
    }
}
