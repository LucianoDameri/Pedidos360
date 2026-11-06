package cl.duoc.pedidos360.ms_analitica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Metricas acumuladas de un producto, calculadas a partir de pedidos.eventos. */
@Entity
@Table(name = "ventas_producto")
@Data
@NoArgsConstructor
public class VentaProducto {

    @Id
    private Long productoId;

    private long pedidosCreados;
    private long pedidosConfirmados;
    private long pedidosCancelados;
    private long unidadesVendidas;

    @Column(precision = 15, scale = 2)
    private BigDecimal ingresos = BigDecimal.ZERO;

    private LocalDateTime actualizadoEn;

    public VentaProducto(Long productoId) {
        this.productoId = productoId;
    }
}
