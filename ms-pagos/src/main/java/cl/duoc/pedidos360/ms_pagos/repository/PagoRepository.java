package cl.duoc.pedidos360.ms_pagos.repository;

import cl.duoc.pedidos360.ms_pagos.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByPedidoId(Long pedidoId);
    List<Pago> findByEstado(Pago.EstadoPago estado);
}
