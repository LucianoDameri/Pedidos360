package cl.duoc.pedidos360.ms_analitica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Alerta de stock recibida desde inventario.alertas (historial para reposicion). */
@Entity
@Table(name = "alertas_stock")
@Data
@NoArgsConstructor
public class AlertaStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 64)
    private String eventId;

    private Long productoId;
    private String tipo;
    private Integer cantidadActual;
    private Integer stockMinimo;
    private String ocurridoEn;
    private LocalDateTime recibidoEn;
}
