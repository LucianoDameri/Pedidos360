package cl.duoc.pedidos360.ms_inventario.controller;

import cl.duoc.pedidos360.ms_inventario.model.Stock;
import cl.duoc.pedidos360.ms_inventario.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventario")
@RequiredArgsConstructor
public class StockController {

    private final StockService service;

    @GetMapping
    public List<Stock> listar() {
        return service.listar();
    }

    @GetMapping("/bajo-stock")
    public List<Stock> listarBajoStock() {
        return service.listarBajoStock();
    }

    @GetMapping("/{id}")
    public Stock obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/producto/{productoId}")
    public Stock obtenerPorProducto(@PathVariable Long productoId) {
        return service.obtenerPorProducto(productoId);
    }

    @PostMapping
    public ResponseEntity<Stock> crear(@Valid @RequestBody Stock stock) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(stock));
    }

    @PutMapping("/{id}")
    public Stock actualizar(@PathVariable Long id, @Valid @RequestBody Stock stock) {
        return service.actualizar(id, stock);
    }

    @PatchMapping("/producto/{productoId}/ajuste")
    public Stock ajustarCantidad(@PathVariable Long productoId,
                                  @RequestParam int delta) {
        return service.ajustarCantidad(productoId, delta);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
