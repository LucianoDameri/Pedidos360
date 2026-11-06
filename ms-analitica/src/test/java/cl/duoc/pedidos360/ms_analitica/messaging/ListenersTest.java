package cl.duoc.pedidos360.ms_analitica.messaging;

import cl.duoc.pedidos360.ms_analitica.exception.EventoInvalidoException;
import cl.duoc.pedidos360.ms_analitica.messaging.dto.AlertaStockEventoDto;
import cl.duoc.pedidos360.ms_analitica.messaging.dto.PedidoEventoDto;
import cl.duoc.pedidos360.ms_analitica.messaging.listener.AlertaStockListener;
import cl.duoc.pedidos360.ms_analitica.messaging.listener.PedidoEventoListener;
import cl.duoc.pedidos360.ms_analitica.service.AlertasService;
import cl.duoc.pedidos360.ms_analitica.service.VentasService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Un listener solo hace dos cosas: llamar al servicio y confirmar el offset DESPUES de que
 * el servicio termino bien. Si el servicio falla, no confirma y deja que decida el manejador de errores.
 */
@ExtendWith(MockitoExtension.class)
class ListenersTest {

    @Mock
    private VentasService ventasService;
    @Mock
    private AlertasService alertasService;
    @Mock
    private Acknowledgment ack;

    private ConsumerRecord<String, PedidoEventoDto> registroPedido(PedidoEventoDto dto) {
        return new ConsumerRecord<>("pedidos.eventos", 1, 42L, "12", dto);
    }

    private PedidoEventoDto pedido() {
        return new PedidoEventoDto("e1", "PEDIDO_CONFIRMADO", 1, 12L, 3L, 7L, 2, new BigDecimal("2000"),
                "CONFIRMADO", "2026-11-02T14:35:10");
    }

    @Test
    void elListenerDePedidosProcesaYLuegoConfirmaElOffset() {
        PedidoEventoDto dto = pedido();

        new PedidoEventoListener(ventasService).onPedidoEvento(registroPedido(dto), ack);

        InOrder orden = inOrder(ventasService, ack);
        orden.verify(ventasService).registrar(dto);
        orden.verify(ack).acknowledge();
    }

    @Test
    void siElServicioFallaElOffsetNoSeConfirmaYElErrorLlegaAlManejador() {
        PedidoEventoDto dto = pedido();
        doThrow(new EventoInvalidoException("mensaje invalido")).when(ventasService).registrar(dto);

        assertThrows(EventoInvalidoException.class,
                () -> new PedidoEventoListener(ventasService).onPedidoEvento(registroPedido(dto), ack));

        verifyNoInteractions(ack);
    }

    @Test
    void elListenerDeAlertasProcesaYLuegoConfirmaElOffset() {
        AlertaStockEventoDto dto = new AlertaStockEventoDto("a1", "STOCK_BAJO", 1, 7L, 3, 5, "2026-11-02T14:36:02");
        ConsumerRecord<String, AlertaStockEventoDto> registro = new ConsumerRecord<>("inventario.alertas", 0, 7L, "7", dto);

        new AlertaStockListener(alertasService).onAlertaStock(registro, ack);

        InOrder orden = inOrder(alertasService, ack);
        orden.verify(alertasService).registrar(dto);
        orden.verify(ack).acknowledge();
    }

    @Test
    void siLaAlertaFallaElOffsetNoSeConfirma() {
        AlertaStockEventoDto dto = new AlertaStockEventoDto("a1", "RARO", 1, 7L, 3, 5, null);
        doThrow(new EventoInvalidoException("tipo desconocido")).when(alertasService).registrar(dto);
        ConsumerRecord<String, AlertaStockEventoDto> registro = new ConsumerRecord<>("inventario.alertas", 0, 7L, "7", dto);

        assertThrows(EventoInvalidoException.class,
                () -> new AlertaStockListener(alertasService).onAlertaStock(registro, ack));

        verifyNoInteractions(ack);
        verify(alertasService).registrar(dto);
    }
}
