package cl.duoc.pedidos360.ms_kafka_admin.exception;

/** El topico o el consumer group indicado no existe en Kafka. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
