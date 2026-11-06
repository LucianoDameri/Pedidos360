package cl.duoc.pedidos360.ms_analitica.repository;

import cl.duoc.pedidos360.ms_analitica.model.AlertaStock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AlertaStockRepository extends JpaRepository<AlertaStock, Long> {

    List<AlertaStock> findAllByOrderByRecibidoEnDescIdDesc(Pageable pageable);

    @Query("select count(distinct a.productoId) from AlertaStock a")
    long contarProductosConAlerta();
}
