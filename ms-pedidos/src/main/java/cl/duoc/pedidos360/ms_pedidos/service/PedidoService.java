package cl.duoc.pedidos360.ms_pedidos.service;

import cl.duoc.pedidos360.ms_pedidos.client.ServiciosClient;
import cl.duoc.pedidos360.ms_pedidos.model.Pedido;
import cl.duoc.pedidos360.ms_pedidos.repository.PedidoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository repository;
    private final ServiciosClient servicios;

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

    /**
     * Crea el pedido: valida cliente y producto, descuenta el stock en inventario
     * y calcula el total con el precio real del producto.
     */
    public Pedido crear(Pedido pedido, String auth) {
        if (servicios.obtenerCliente(pedido.getClienteId(), auth) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cliente no existe");
        }

        Map<String, Object> producto = servicios.obtenerProducto(pedido.getProductoId(), auth);
        if (producto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto no existe");
        }
        if (!Boolean.TRUE.equals(producto.get("activo"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El producto no está disponible");
        }
        BigDecimal precio = new BigDecimal(String.valueOf(producto.get("precio")));

        // Descuenta stock (409 si no alcanza)
        servicios.ajustarStock(pedido.getProductoId(), -pedido.getCantidad(), auth);

        pedido.setId(null);
        pedido.setEstado(Pedido.EstadoPedido.PENDIENTE);
        pedido.setFechaCreacion(LocalDateTime.now());
        pedido.setTotal(precio.multiply(BigDecimal.valueOf(pedido.getCantidad())));
        try {
            return repository.save(pedido);
        } catch (RuntimeException e) {
            reponerStock(pedido, auth); // si no se pudo guardar, se devuelve el stock
            throw e;
        }
    }

    /** Cancelar un pedido pendiente repone el stock. Un pedido pagado no se puede cancelar. */
    public Pedido actualizarEstado(Long id, Pedido.EstadoPedido nuevoEstado, String auth) {
        Pedido pedido = obtener(id);
        if (nuevoEstado == Pedido.EstadoPedido.CANCELADO
                && pedido.getEstado() != Pedido.EstadoPedido.CANCELADO) {
            if (pedido.getEstado() != Pedido.EstadoPedido.PENDIENTE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Solo se puede cancelar un pedido pendiente de pago");
            }
            servicios.ajustarStock(pedido.getProductoId(), pedido.getCantidad(), auth);
        }
        pedido.setEstado(nuevoEstado);
        return repository.save(pedido);
    }

    /** Solo se permite cambiar la dirección de entrega (producto y cantidad afectan el stock). */
    public Pedido actualizar(Long id, Pedido datos) {
        Pedido existente = obtener(id);
        existente.setDireccionEntrega(datos.getDireccionEntrega());
        return repository.save(existente);
    }

    /** Eliminar un pedido pendiente repone el stock; uno ya pagado no se puede eliminar. */
    public void eliminar(Long id, String auth) {
        Pedido pedido = obtener(id);
        if (pedido.getEstado() == Pedido.EstadoPedido.PENDIENTE) {
            servicios.ajustarStock(pedido.getProductoId(), pedido.getCantidad(), auth);
        } else if (pedido.getEstado() != Pedido.EstadoPedido.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar un pedido ya pagado");
        }
        repository.deleteById(id);
    }

    private void reponerStock(Pedido pedido, String auth) {
        try {
            servicios.ajustarStock(pedido.getProductoId(), pedido.getCantidad(), auth);
        } catch (RuntimeException ignorado) {
            // se informa el error original
        }
    }
}
