package cl.duoc.pedidos360.ms_productos.service;

import cl.duoc.pedidos360.ms_productos.model.Producto;
import cl.duoc.pedidos360.ms_productos.repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository repository;

    public List<Producto> listar() {
        return repository.findAll();
    }

    public List<Producto> listarActivos() {
        return repository.findByActivoTrue();
    }

    public List<Producto> listarPorCategoria(String categoria) {
        return repository.findByCategoria(categoria);
    }

    public Producto obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));
    }

    public Producto crear(Producto producto) {
        return repository.save(producto);
    }

    public Producto actualizar(Long id, Producto datos) {
        Producto existente = obtener(id);
        existente.setNombre(datos.getNombre());
        existente.setDescripcion(datos.getDescripcion());
        existente.setPrecio(datos.getPrecio());
        existente.setCategoria(datos.getCategoria());
        existente.setActivo(datos.isActivo());
        return repository.save(existente);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
