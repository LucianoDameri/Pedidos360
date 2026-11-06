package cl.duoc.pedidos360.ms_clientes.service;

import cl.duoc.pedidos360.ms_clientes.messaging.event.PagoEvent;
import cl.duoc.pedidos360.ms_clientes.messaging.event.PedidoEvent;
import cl.duoc.pedidos360.ms_clientes.model.Cliente;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Logica de negocio de las notificaciones (simula el envio de correo).
 * No sabe nada de RabbitMQ. Lanza EntityNotFoundException si el cliente no existe.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final ClienteService clienteService;

    public String notificarPedido(PedidoEvent evento) {
        Cliente cliente = clienteService.obtener(evento.clienteId());
        String texto = "PEDIDO_CANCELADO".equals(evento.tipo())
                ? "tu pedido #" + evento.pedidoId() + " fue cancelado"
                : "recibimos tu pedido #" + evento.pedidoId() + " por un total de $" + evento.total()
                        + ". Queda pendiente de pago";
        return enviar(cliente, texto);
    }

    public String notificarPago(PagoEvent evento) {
        Cliente cliente = clienteService.obtener(evento.clienteId());
        String texto = "tu pago de $" + evento.monto() + " del pedido #" + evento.pedidoId()
                + " fue aprobado (ref. " + evento.referencia() + ")";
        return enviar(cliente, texto);
    }

    private String enviar(Cliente cliente, String texto) {
        String mensaje = "Hola " + cliente.getNombre() + ", " + texto;
        log.info("[NOTIFICACION] Correo enviado a {} -> {}", cliente.getEmail(), mensaje);
        return mensaje;
    }
}
