package cl.duoc.pedidos360.ms_analitica.exception;

/**
 * El mensaje de Kafka llego bien pero su contenido no sirve (faltan campos, valores imposibles).
 * Reintentarlo no lo arregla: el manejador de errores lo envia directo a la DLT.
 * Extiende IllegalArgumentException, que esta marcada como NO reintentable.
 */
public class EventoInvalidoException extends IllegalArgumentException {

    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
