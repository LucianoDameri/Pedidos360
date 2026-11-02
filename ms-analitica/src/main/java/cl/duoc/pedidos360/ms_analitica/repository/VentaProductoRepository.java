package cl.duoc.pedidos360.ms_analitica.repository;

import cl.duoc.pedidos360.ms_analitica.model.VentaProducto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VentaProductoRepository extends JpaRepository<VentaProducto, Long> {

    /**
     * Bloquea la fila mientras se actualiza: los 3 hilos del consumer group pueden procesar
     * eventos del mismo producto (de pedidos distintos, en particiones distintas) a la vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VentaProducto v where v.productoId = :id")
    Optional<VentaProducto> buscarParaActualizar(@Param("id") Long id);

    List<VentaProducto> findAllByOrderByUnidadesVendidasDescProductoIdAsc();

    List<VentaProducto> findAllByOrderByUnidadesVendidasDescProductoIdAsc(Pageable pageable);
}
