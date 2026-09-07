package cl.duoc.pedidos360.ms_pagos.service;

import cl.duoc.pedidos360.ms_pagos.model.Pago;
import cl.duoc.pedidos360.ms_pagos.repository.PagoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository repository;

    public List<Pago> listar() {
        return repository.findAll();
    }

    public List<Pago> listarPorEstado(Pago.EstadoPago estado) {
        return repository.findByEstado(estado);
    }

    public Pago obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pago no encontrado con id: " + id));
    }

    public Pago obtenerPorPedido(Long pedidoId) {
        return repository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException("Pago no encontrado para pedidoId: " + pedidoId));
    }

    public Pago procesar(Pago pago) {
        pago.setEstado(Pago.EstadoPago.PROCESANDO);
        pago.setReferencia(UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        Pago guardado = repository.save(pago);

        // Simulación de procesamiento: aprobación automática
        guardado.setEstado(Pago.EstadoPago.APROBADO);
        guardado.setFechaPago(LocalDateTime.now());
        return repository.save(guardado);
    }

    public Pago actualizarEstado(Long id, Pago.EstadoPago nuevoEstado) {
        Pago pago = obtener(id);
        pago.setEstado(nuevoEstado);
        if (nuevoEstado == Pago.EstadoPago.APROBADO) {
            pago.setFechaPago(LocalDateTime.now());
        }
        return repository.save(pago);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
