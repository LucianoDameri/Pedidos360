package cl.duoc.pedidos360.ms_inventario.service;

import cl.duoc.pedidos360.ms_inventario.messaging.event.PedidoEvent;
import cl.duoc.pedidos360.ms_inventario.model.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Logica de negocio de las alertas de stock. No sabe nada de RabbitMQ.
 * Lanza EntityNotFoundException si el producto no tiene registro de stock.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertaStockService {

    private final StockService stockService;

    /** Revisa el stock del producto del pedido; devuelve true si quedo en nivel bajo. */
    public boolean evaluar(PedidoEvent evento) {
        Stock stock = stockService.obtenerPorProducto(evento.productoId());
        if (stock.esBajoStock()) {
            log.warn("[ALERTA STOCK] Producto {} con stock bajo: {} unidades (minimo {}). Motivo: {} del pedido #{}",
                    stock.getProductoId(), stock.getCantidad(), stock.getStockMinimo(),
                    evento.tipo(), evento.pedidoId());
            return true;
        }
        log.info("[STOCK] Producto {} con stock normal: {} unidades (evento {} del pedido #{})",
                stock.getProductoId(), stock.getCantidad(), evento.tipo(), evento.pedidoId());
        return false;
    }
}
