package cl.duoc.pedidos360.ms_pedidos.service;

import cl.duoc.pedidos360.ms_pedidos.messaging.event.PagoEvent;
import cl.duoc.pedidos360.ms_pedidos.model.Pedido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Logica de negocio del ticket: no sabe nada de RabbitMQ. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final PedidoService pedidoService;

    /** Genera el ticket de un pedido pagado. Lanza EntityNotFoundException si el pedido no existe. */
    public String generar(PagoEvent pago) {
        Pedido pedido = pedidoService.obtener(pago.pedidoId());
        String ticket = String.format(
                "TICKET-%06d | pedido=%d | cliente=%d | producto=%d x%d | total=%s | metodo=%s | ref=%s",
                pedido.getId(), pedido.getId(), pedido.getClienteId(), pedido.getProductoId(),
                pedido.getCantidad(), pedido.getTotal(), pago.metodoPago(), pago.referencia());
        log.info("[TICKET] {}", ticket);
        return ticket;
    }
}
