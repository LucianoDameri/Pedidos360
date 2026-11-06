package cl.duoc.pedidos360.ms_pagos.service;

import cl.duoc.pedidos360.ms_pagos.client.ServiciosClient;
import cl.duoc.pedidos360.ms_pagos.messaging.publisher.PagoEventPublisher;
import cl.duoc.pedidos360.ms_pagos.model.Pago;
import cl.duoc.pedidos360.ms_pagos.repository.PagoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository repository;
    private final ServiciosClient servicios;
    private final PagoEventPublisher eventos; // mensajeria asincrona (RabbitMQ)

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

    /**
     * Paga un pedido: el monto es el total del pedido (no lo decide el cliente).
     * Si el pago se aprueba, el pedido pasa a CONFIRMADO.
     */
    public Pago procesar(Pago pago, String auth) {
        Map<String, Object> pedido = servicios.obtenerPedido(pago.getPedidoId(), auth);
        if (pedido == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El pedido no existe");
        }
        if (!"PENDIENTE".equals(String.valueOf(pedido.get("estado")))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido no está pendiente de pago");
        }
        if (repository.findByPedidoId(pago.getPedidoId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pedido ya tiene un pago registrado");
        }
        if (pago.getMetodoPago() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes indicar el método de pago");
        }

        pago.setId(null);
        pago.setMonto(new BigDecimal(String.valueOf(pedido.get("total"))));
        pago.setEstado(Pago.EstadoPago.PROCESANDO);
        pago.setReferencia(UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        Pago guardado = repository.save(pago);

        // Simulación de procesamiento: aprobación automática
        guardado.setEstado(Pago.EstadoPago.APROBADO);
        guardado.setFechaPago(LocalDateTime.now());
        guardado = repository.save(guardado);

        try {
            servicios.confirmarPedido(pago.getPedidoId(), auth);
        } catch (RuntimeException e) {
            repository.delete(guardado); // sin confirmar el pedido no queda pago registrado
            throw e;
        }

        // Evento asincrono: notificacion al cliente y generacion del ticket
        Object clienteId = pedido.get("clienteId");
        eventos.pagoAprobado(guardado, clienteId != null ? Long.valueOf(String.valueOf(clienteId)) : null);
        return guardado;
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
