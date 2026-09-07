package cl.duoc.pedidos360.ms_productos.repository;

import cl.duoc.pedidos360.ms_productos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByCategoria(String categoria);
    List<Producto> findByActivoTrue();
}
