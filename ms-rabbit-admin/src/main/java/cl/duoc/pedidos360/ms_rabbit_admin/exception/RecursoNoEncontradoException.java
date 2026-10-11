package cl.duoc.pedidos360.ms_rabbit_admin.exception;

/** La cola o el exchange indicado no existe en RabbitMQ. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
