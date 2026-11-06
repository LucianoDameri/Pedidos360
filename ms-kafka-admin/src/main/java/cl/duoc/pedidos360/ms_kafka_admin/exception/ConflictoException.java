package cl.duoc.pedidos360.ms_kafka_admin.exception;

/** La operacion choca con el estado actual (el topico ya existe, el grupo tiene miembros activos). */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
