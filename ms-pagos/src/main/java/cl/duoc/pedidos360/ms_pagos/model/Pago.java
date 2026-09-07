package cl.duoc.pedidos360.ms_pagos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El ID del pedido es obligatorio")
    @Column(unique = true)
    private Long pedidoId;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    private MetodoPago metodoPago;

    @Enumerated(EnumType.STRING)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    private LocalDateTime fechaPago;

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    private String referencia; // código de transacción externo

    public enum MetodoPago {
        TARJETA_CREDITO, TARJETA_DEBITO, TRANSFERENCIA, EFECTIVO
    }

    public enum EstadoPago {
        PENDIENTE, PROCESANDO, APROBADO, RECHAZADO, REEMBOLSADO
    }
}
