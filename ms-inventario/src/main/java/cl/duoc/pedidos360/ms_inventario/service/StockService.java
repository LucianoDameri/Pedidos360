package cl.duoc.pedidos360.ms_inventario.service;

import cl.duoc.pedidos360.ms_inventario.model.Stock;
import cl.duoc.pedidos360.ms_inventario.repository.StockRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository repository;

    public List<Stock> listar() {
        return repository.findAll();
    }

    public List<Stock> listarBajoStock() {
        return repository.findBajoStock();
    }

    public Stock obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Stock no encontrado con id: " + id));
    }

    public Stock obtenerPorProducto(Long productoId) {
        return repository.findByProductoId(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Stock no encontrado para productoId: " + productoId));
    }

    public Stock crear(Stock stock) {
        return repository.save(stock);
    }

    public Stock actualizar(Long id, Stock datos) {
        Stock existente = obtener(id);
        existente.setCantidad(datos.getCantidad());
        existente.setStockMinimo(datos.getStockMinimo());
        return repository.save(existente);
    }

    public Stock ajustarCantidad(Long productoId, int delta) {
        Stock stock = obtenerPorProducto(productoId);
        int nuevaCantidad = stock.getCantidad() + delta;
        if (nuevaCantidad < 0) {
            throw new IllegalStateException("Stock insuficiente para el producto: " + productoId);
        }
        stock.setCantidad(nuevaCantidad);
        return repository.save(stock);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
