package cl.duoc.pedidos360.ms_inventario.messaging;

import cl.duoc.pedidos360.ms_inventario.config.KafkaTopicsProperties;
import cl.duoc.pedidos360.ms_inventario.messaging.event.AlertaStockEventoDto;
import cl.duoc.pedidos360.ms_inventario.messaging.publisher.AlertaStockKafkaPublisher;
import cl.duoc.pedidos360.ms_inventario.model.Stock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaStockKafkaPublisherTest {

    @Mock
    private KafkaTemplate<String, AlertaStockEventoDto> kafkaTemplate;

    private AlertaStockKafkaPublisher publisher;

    @BeforeEach
    void preparar() {
        KafkaTopicsProperties props = new KafkaTopicsProperties();
        props.getTopics().getInventarioAlertas().setName("inventario.alertas");
        publisher = new AlertaStockKafkaPublisher(kafkaTemplate, props);
    }

    private Stock stock(int cantidad) {
        Stock s = new Stock();
        s.setProductoId(7L);
        s.setCantidad(cantidad);
        s.setStockMinimo(5);
        return s;
    }

    @Test
    void publicaEnElTopicoConfiguradoUsandoElProductoIdComoClave() {
        when(kafkaTemplate.send(anyString(), anyString(), any(AlertaStockEventoDto.class)))
                .thenReturn(new CompletableFuture<>());

        publisher.publicar(stock(3));

        ArgumentCaptor<AlertaStockEventoDto> evento = ArgumentCaptor.forClass(AlertaStockEventoDto.class);
        verify(kafkaTemplate).send(eq("inventario.alertas"), eq("7"), evento.capture());
        assertEquals("STOCK_BAJO", evento.getValue().tipo());
        assertEquals(7L, evento.getValue().productoId());
        assertEquals(3, evento.getValue().cantidadActual());
        assertEquals(5, evento.getValue().stockMinimo());
    }

    @Test
    void conStockCeroLaAlertaEsStockAgotado() {
        assertEquals("STOCK_AGOTADO", AlertaStockEventoDto.desde(stock(0)).tipo());
        assertEquals("STOCK_BAJO", AlertaStockEventoDto.desde(stock(1)).tipo());
    }

    @Test
    void siKafkaFallaElAjusteDeStockNoSeVeAfectado() {
        when(kafkaTemplate.send(anyString(), anyString(), any(AlertaStockEventoDto.class)))
                .thenThrow(new IllegalStateException("sin brokers"));

        assertDoesNotThrow(() -> publisher.publicar(stock(3)));
    }

    @Test
    void elJsonDelMensajeCumpleElContrato() throws Exception {
        AlertaStockEventoDto dto = AlertaStockEventoDto.desde(stock(3));

        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(dto));

        assertEquals("STOCK_BAJO", json.get("tipo").asText());
        assertEquals(1, json.get("version").asInt());
        assertEquals(7, json.get("productoId").asInt());
        assertEquals(3, json.get("cantidadActual").asInt());
        assertEquals(5, json.get("stockMinimo").asInt());
        assertEquals(false, json.get("eventId").isNull());
        assertFalse(json.has("@class"));
    }
}
