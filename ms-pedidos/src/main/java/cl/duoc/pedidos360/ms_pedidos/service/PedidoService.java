package cl.duoc.pedidos360.ms_pedidos.service;

import cl.duoc.pedidos360.ms_pedidos.model.Pedido;
import cl.duoc.pedidos360.ms_pedidos.repository.PedidoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository repository;

    public List<Pedido> listar() {
        return repository.findAll();
    }

    public List<Pedido> listarPorCliente(Long clienteId) {
        return repository.findByClienteId(clienteId);
    }

    public List<Pedido> listarPorEstado(Pedido.EstadoPedido estado) {
        return repository.findByEstado(estado);
    }

    public Pedido obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado con id: " + id));
    }

    public Pedido crear(Pedido pedido) {
        pedido.setEstado(Pedido.EstadoPedido.PENDIENTE);
        return repository.save(pedido);
    }

    public Pedido actualizarEstado(Long id, Pedido.EstadoPedido nuevoEstado) {
        Pedido pedido = obtener(id);
        pedido.setEstado(nuevoEstado);
        return repository.save(pedido);
    }

    public Pedido actualizar(Long id, Pedido datos) {
        Pedido existente = obtener(id);
        existente.setClienteId(datos.getClienteId());
        existente.setProductoId(datos.getProductoId());
        existente.setCantidad(datos.getCantidad());
        existente.setTotal(datos.getTotal());
        existente.setDireccionEntrega(datos.getDireccionEntrega());
        return repository.save(existente);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
