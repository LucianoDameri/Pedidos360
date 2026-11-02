package cl.duoc.pedidos360.ms_kafka_admin.exception;

/** Kafka no respondio a tiempo (brokers caidos o inalcanzables). */
public class KafkaNoDisponibleException extends RuntimeException {

    public KafkaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
