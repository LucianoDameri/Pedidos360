package cl.duoc.pedidos360.ms_pedidos.repository;

import cl.duoc.pedidos360.ms_pedidos.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByClienteId(Long clienteId);
    List<Pedido> findByEstado(Pedido.EstadoPedido estado);
}
