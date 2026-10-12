package cl.duoc.pedidos360.ms_inventario.config;

import cl.duoc.pedidos360.ms_inventario.model.Stock;
import cl.duoc.pedidos360.ms_inventario.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Carga el stock inicial de los 12 productos del catálogo al arrancar, solo si la tabla está vacía.
 * Los ids de producto (1..12) corresponden al orden de CatalogoInicial en ms-productos
 * y de docs/seed-catalogo.sql. No pisa nunca el stock que ya exista.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockInicial implements CommandLineRunner {

    private final StockRepository repository;

    // { productoId, cantidad, stockMinimo }
    private static final int[][] STOCK = {
            {1, 8, 3}, {2, 40, 10}, {3, 15, 5}, {4, 12, 4}, {5, 25, 8}, {6, 10, 3},
            {7, 14, 5}, {8, 60, 15}, {9, 9, 3}, {10, 20, 6}, {11, 6, 2}, {12, 5, 2}
    };

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        for (int[] fila : STOCK) {
            Stock stock = new Stock();
            stock.setProductoId((long) fila[0]);
            stock.setCantidad(fila[1]);
            stock.setStockMinimo(fila[2]);
            repository.save(stock);
        }
        log.info("[STOCK] Base de datos vacía: se cargó el stock inicial de {} productos", STOCK.length);
    }
}
