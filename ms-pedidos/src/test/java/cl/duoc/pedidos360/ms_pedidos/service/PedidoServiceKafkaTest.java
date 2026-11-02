package cl.duoc.pedidos360.ms_pedidos.service;

import cl.duoc.pedidos360.ms_pedidos.client.ServiciosClient;
import cl.duoc.pedidos360.ms_pedidos.messaging.publisher.PedidoEventPublisher;
import cl.duoc.pedidos360.ms_pedidos.messaging.publisher.PedidoKafkaPublisher;
import cl.duoc.pedidos360.ms_pedidos.model.Pedido;
import cl.duoc.pedidos360.ms_pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Verifica que PedidoService publique en Kafka cada cambio relevante del pedido. */
@ExtendWith(MockitoExtension.class)
class PedidoServiceKafkaTest {

    private static final String AUTH = "Bearer token-de-prueba";

    @Mock
    private PedidoRepository repository;
    @Mock
    private ServiciosClient servicios;
    @Mock
    private PedidoEventPublisher rabbit;
    @Mock
    private PedidoKafkaPublisher kafka;
    @InjectMocks
    private PedidoService service;

    private Pedido pedidoEnEstado(Pedido.EstadoPedido estado) {
        Pedido p = new Pedido();
        p.setId(12L);
        p.setClienteId(3L);
        p.setProductoId(7L);
        p.setCantidad(2);
        p.setTotal(new BigDecimal("2000"));
        p.setEstado(estado);
        return p;
    }

    @Test
    void crearUnPedidoPublicaPedidoCreadoEnKafka() {
        Pedido entrada = new Pedido();
        entrada.setClienteId(3L);
        entrada.setProductoId(7L);
        entrada.setCantidad(2);
        when(servicios.obtenerCliente(3L, AUTH)).thenReturn(Map.of("id", 3));
        when(servicios.obtenerProducto(7L, AUTH)).thenReturn(Map.of("activo", true, "precio", 1000));
        when(repository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido guardado = inv.getArgument(0);
            guardado.setId(12L);
            return guardado;
        });

        Pedido resultado = service.crear(entrada, AUTH);

        assertEquals(new BigDecimal("2000"), resultado.getTotal());
        verify(servicios).ajustarStock(7L, -2, AUTH);
        verify(rabbit).pedidoCreado(resultado);
        verify(kafka).publicar("PEDIDO_CREADO", resultado);
    }

    @Test
    void siElProductoNoExisteNoSePublicaNada() {
        Pedido entrada = new Pedido();
        entrada.setClienteId(3L);
        entrada.setProductoId(99L);
        entrada.setCantidad(1);
        when(servicios.obtenerCliente(3L, AUTH)).thenReturn(Map.of("id", 3));
        when(servicios.obtenerProducto(99L, AUTH)).thenReturn(null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.crear(entrada, AUTH));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(kafka);
        verifyNoInteractions(rabbit);
    }

    @Test
    void confirmarUnPedidoPublicaPedidoConfirmado() {
        Pedido pedido = pedidoEnEstado(Pedido.EstadoPedido.PENDIENTE);
        when(repository.findById(12L)).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        service.actualizarEstado(12L, Pedido.EstadoPedido.CONFIRMADO, AUTH);

        verify(kafka).publicar("PEDIDO_CONFIRMADO", pedido);
        verify(rabbit, never()).pedidoCancelado(any());
    }

    @Test
    void siElEstadoNoCambiaNoSePublicaNada() {
        Pedido pedido = pedidoEnEstado(Pedido.EstadoPedido.CONFIRMADO);
        when(repository.findById(12L)).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        service.actualizarEstado(12L, Pedido.EstadoPedido.CONFIRMADO, AUTH);

        verify(kafka, never()).publicar(anyString(), any());
    }

    @Test
    void cancelarUnPedidoPendienteReponeStockYPublicaEnRabbitYKafka() {
        Pedido pedido = pedidoEnEstado(Pedido.EstadoPedido.PENDIENTE);
        when(repository.findById(12L)).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        service.actualizarEstado(12L, Pedido.EstadoPedido.CANCELADO, AUTH);

        verify(servicios).ajustarStock(7L, 2, AUTH);
        verify(rabbit).pedidoCancelado(pedido);
        verify(kafka).publicar("PEDIDO_CANCELADO", pedido);
    }

    @Test
    void noSePuedeCancelarUnPedidoYaPagadoYNoSePublicaNada() {
        Pedido pedido = pedidoEnEstado(Pedido.EstadoPedido.CONFIRMADO);
        when(repository.findById(12L)).thenReturn(Optional.of(pedido));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.actualizarEstado(12L, Pedido.EstadoPedido.CANCELADO, AUTH));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verifyNoInteractions(kafka);
    }
}
