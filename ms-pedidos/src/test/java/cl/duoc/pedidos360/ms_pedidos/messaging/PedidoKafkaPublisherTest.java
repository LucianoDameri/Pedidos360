package cl.duoc.pedidos360.ms_pedidos.messaging;

import cl.duoc.pedidos360.ms_pedidos.config.KafkaTopicsProperties;
import cl.duoc.pedidos360.ms_pedidos.messaging.event.PedidoEventoDto;
import cl.duoc.pedidos360.ms_pedidos.messaging.publisher.PedidoKafkaPublisher;
import cl.duoc.pedidos360.ms_pedidos.model.Pedido;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoKafkaPublisherTest {

    @Mock
    private KafkaTemplate<String, PedidoEventoDto> kafkaTemplate;

    private PedidoKafkaPublisher publisher;

    @BeforeEach
    void preparar() {
        KafkaTopicsProperties props = new KafkaTopicsProperties();
        props.getTopics().getPedidosEventos().setName("pedidos.eventos");
        publisher = new PedidoKafkaPublisher(kafkaTemplate, props);
    }

    private Pedido pedido() {
        Pedido p = new Pedido();
        p.setId(12L);
        p.setClienteId(3L);
        p.setProductoId(7L);
        p.setCantidad(2);
        p.setTotal(new BigDecimal("159980"));
        p.setEstado(Pedido.EstadoPedido.CONFIRMADO);
        return p;
    }

    @Test
    void publicaEnElTopicoConfiguradoUsandoElPedidoIdComoClave() {
        when(kafkaTemplate.send(anyString(), anyString(), any(PedidoEventoDto.class)))
                .thenReturn(new CompletableFuture<>());

        publisher.publicar("PEDIDO_CONFIRMADO", pedido());

        ArgumentCaptor<PedidoEventoDto> evento = ArgumentCaptor.forClass(PedidoEventoDto.class);
        verify(kafkaTemplate).send(eq("pedidos.eventos"), eq("12"), evento.capture());
        assertEquals("PEDIDO_CONFIRMADO", evento.getValue().tipo());
        assertEquals(12L, evento.getValue().pedidoId());
        assertEquals(7L, evento.getValue().productoId());
        assertEquals(2, evento.getValue().cantidad());
        assertEquals("CONFIRMADO", evento.getValue().estado());
        assertEquals(PedidoEventoDto.VERSION, evento.getValue().version());
    }

    @Test
    void siKafkaLanzaUnaExcepcionLaCompraNoFalla() {
        when(kafkaTemplate.send(anyString(), anyString(), any(PedidoEventoDto.class)))
                .thenThrow(new IllegalStateException("sin brokers"));

        assertDoesNotThrow(() -> publisher.publicar("PEDIDO_CREADO", pedido()));
    }

    @Test
    void siElEnvioFallaDespuesDeEncolarseTampocoSePropagaElError() {
        CompletableFuture<org.springframework.kafka.support.SendResult<String, PedidoEventoDto>> futuro = new CompletableFuture<>();
        futuro.completeExceptionally(new RuntimeException("el broker no respondio"));
        when(kafkaTemplate.send(anyString(), anyString(), any(PedidoEventoDto.class))).thenReturn(futuro);

        assertDoesNotThrow(() -> publisher.publicar("PEDIDO_CREADO", pedido()));
    }

    @Test
    void cadaEventoTieneUnIdentificadorUnico() {
        PedidoEventoDto a = PedidoEventoDto.desde("PEDIDO_CREADO", pedido());
        PedidoEventoDto b = PedidoEventoDto.desde("PEDIDO_CREADO", pedido());
        assertNotNull(a.eventId());
        assertNotEquals(a.eventId(), b.eventId());
    }

    @Test
    void elJsonDelMensajeCumpleElContratoSinCabecerasDeTipoJava() throws Exception {
        PedidoEventoDto dto = PedidoEventoDto.desde("PEDIDO_CONFIRMADO", pedido());

        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(dto));

        assertEquals("PEDIDO_CONFIRMADO", json.get("tipo").asText());
        assertEquals(1, json.get("version").asInt());
        assertEquals(12, json.get("pedidoId").asInt());
        assertEquals(3, json.get("clienteId").asInt());
        assertEquals(7, json.get("productoId").asInt());
        assertEquals(2, json.get("cantidad").asInt());
        assertEquals(159980, json.get("total").asInt());
        assertEquals("CONFIRMADO", json.get("estado").asText());
        assertNotNull(json.get("eventId"));
        assertNotNull(json.get("ocurridoEn"));
        assertFalse(json.has("@class"));
    }
}
