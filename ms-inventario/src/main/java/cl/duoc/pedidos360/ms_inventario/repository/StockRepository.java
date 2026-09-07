package cl.duoc.pedidos360.ms_inventario.repository;

import cl.duoc.pedidos360.ms_inventario.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByProductoId(Long productoId);

    @Query("SELECT s FROM Stock s WHERE s.cantidad <= s.stockMinimo")
    List<Stock> findBajoStock();
}
