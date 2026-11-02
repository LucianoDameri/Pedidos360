package cl.duoc.pedidos360.ms_analitica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registro de los eventos ya procesados. Kafka entrega "al menos una vez": si un mensaje llega
 * dos veces (reintento, rebalanceo) el eventId permite detectarlo y no contarlo dos veces.
 */
@Entity
@Table(name = "eventos_procesados")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoProcesado {

    @Id
    @Column(length = 64)
    private String eventId;

    private String topico;

    private LocalDateTime procesadoEn;
}
