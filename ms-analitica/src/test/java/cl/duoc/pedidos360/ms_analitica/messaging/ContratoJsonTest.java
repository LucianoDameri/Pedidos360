package cl.duoc.pedidos360.ms_analitica.messaging;

import cl.duoc.pedidos360.ms_analitica.messaging.dto.AlertaStockEventoDto;
import cl.duoc.pedidos360.ms_analitica.messaging.dto.PedidoEventoDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Prueba el modelo de mensajes JSON de entrada de los dos topicos (docs/KAFKA.md). */
class ContratoJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void leeElJsonDePedidosEventosIgnorandoCamposNuevos() throws Exception {
        String json = "{\"eventId\":\"6f1c\",\"tipo\":\"PEDIDO_CONFIRMADO\",\"version\":1,\"pedidoId\":12,"
                + "\"clienteId\":3,\"productoId\":7,\"cantidad\":2,\"total\":159980,\"estado\":\"CONFIRMADO\","
                + "\"ocurridoEn\":\"2026-11-02T14:35:10\",\"campoNuevoDelFuturo\":\"x\"}";

        PedidoEventoDto dto = mapper.readValue(json, PedidoEventoDto.class);

        assertEquals("6f1c", dto.eventId());
        assertEquals(PedidoEventoDto.CONFIRMADO, dto.tipo());
        assertEquals(12L, dto.pedidoId());
        assertEquals(7L, dto.productoId());
        assertEquals(2, dto.cantidad());
        assertEquals(0, new BigDecimal("159980").compareTo(dto.total()));
    }

    @Test
    void leeElJsonDeInventarioAlertas() throws Exception {
        String json = "{\"eventId\":\"a41d\",\"tipo\":\"STOCK_BAJO\",\"version\":1,\"productoId\":7,"
                + "\"cantidadActual\":3,\"stockMinimo\":5,\"ocurridoEn\":\"2026-11-02T14:36:02\"}";

        AlertaStockEventoDto dto = mapper.readValue(json, AlertaStockEventoDto.class);

        assertEquals(AlertaStockEventoDto.STOCK_BAJO, dto.tipo());
        assertEquals(7L, dto.productoId());
        assertEquals(3, dto.cantidadActual());
        assertEquals(5, dto.stockMinimo());
    }

    @Test
    void unJsonRotoFallaAlLeerse() {
        assertThrows(JsonProcessingException.class,
                () -> mapper.readValue("{\"esto no es un evento valido\"", PedidoEventoDto.class));
    }
}
