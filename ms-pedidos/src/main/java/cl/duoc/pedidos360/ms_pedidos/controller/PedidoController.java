package cl.duoc.pedidos360.ms_pedidos.controller;

import cl.duoc.pedidos360.ms_pedidos.model.Pedido;
import cl.duoc.pedidos360.ms_pedidos.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService service;

    @GetMapping
    public List<Pedido> listar() {
        return service.listar();
    }

    @GetMapping("/cliente/{clienteId}")
    public List<Pedido> listarPorCliente(@PathVariable Long clienteId) {
        return service.listarPorCliente(clienteId);
    }

    @GetMapping("/estado/{estado}")
    public List<Pedido> listarPorEstado(@PathVariable Pedido.EstadoPedido estado) {
        return service.listarPorEstado(estado);
    }

    @GetMapping("/{id}")
    public Pedido obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Pedido> crear(@Valid @RequestBody Pedido pedido,
                                        @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(pedido, auth));
    }

    @PutMapping("/{id}")
    public Pedido actualizar(@PathVariable Long id, @RequestBody Pedido pedido) {
        return service.actualizar(id, pedido);
    }

    @PatchMapping("/{id}/estado")
    public Pedido actualizarEstado(@PathVariable Long id,
                                   @RequestParam Pedido.EstadoPedido estado,
                                   @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return service.actualizarEstado(id, estado, auth);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                         @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        service.eliminar(id, auth);
        return ResponseEntity.noContent().build();
    }
}
