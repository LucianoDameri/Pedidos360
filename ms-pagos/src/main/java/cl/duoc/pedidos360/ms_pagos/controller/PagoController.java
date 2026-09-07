package cl.duoc.pedidos360.ms_pagos.controller;

import cl.duoc.pedidos360.ms_pagos.model.Pago;
import cl.duoc.pedidos360.ms_pagos.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService service;

    @GetMapping
    public List<Pago> listar() {
        return service.listar();
    }

    @GetMapping("/estado/{estado}")
    public List<Pago> listarPorEstado(@PathVariable Pago.EstadoPago estado) {
        return service.listarPorEstado(estado);
    }

    @GetMapping("/{id}")
    public Pago obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/pedido/{pedidoId}")
    public Pago obtenerPorPedido(@PathVariable Long pedidoId) {
        return service.obtenerPorPedido(pedidoId);
    }

    @PostMapping
    public ResponseEntity<Pago> procesar(@Valid @RequestBody Pago pago) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.procesar(pago));
    }

    @PatchMapping("/{id}/estado")
    public Pago actualizarEstado(@PathVariable Long id,
                                  @RequestParam Pago.EstadoPago estado) {
        return service.actualizarEstado(id, estado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
