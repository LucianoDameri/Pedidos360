package cl.duoc.pedidos360.ms_analitica.service;

import cl.duoc.pedidos360.ms_analitica.exception.EventoInvalidoException;
import cl.duoc.pedidos360.ms_analitica.messaging.dto.AlertaStockEventoDto;
import cl.duoc.pedidos360.ms_analitica.model.AlertaStock;
import cl.duoc.pedidos360.ms_analitica.model.EventoProcesado;
import cl.duoc.pedidos360.ms_analitica.repository.AlertaStockRepository;
import cl.duoc.pedidos360.ms_analitica.repository.EventoProcesadoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertasServiceTest {

    @Mock
    private AlertaStockRepository alertas;
    @Mock
    private EventoProcesadoRepository procesados;
    @InjectMocks
    private AlertasService service;

    private AlertaStockEventoDto alerta(String id, String tipo, Integer cantidad) {
        return new AlertaStockEventoDto(id, tipo, 1, 7L, cantidad, 5, "2026-11-02T14:36:02");
    }

    @Test
    void guardaLaAlertaYMarcaElEventoComoProcesado() {
        when(procesados.existsById("a1")).thenReturn(false);

        boolean guardada = service.registrar(alerta("a1", "STOCK_BAJO", 3));

        assertTrue(guardada);
        ArgumentCaptor<AlertaStock> captura = ArgumentCaptor.forClass(AlertaStock.class);
        verify(alertas).save(captura.capture());
        assertEquals("a1", captura.getValue().getEventId());
        assertEquals(7L, captura.getValue().getProductoId());
        assertEquals("STOCK_BAJO", captura.getValue().getTipo());
        assertEquals(3, captura.getValue().getCantidadActual());
        assertNotNull(captura.getValue().getRecibidoEn());
        verify(procesados).save(any(EventoProcesado.class));
    }

    @Test
    void unaAlertaDuplicadaSeIgnora() {
        when(procesados.existsById("a2")).thenReturn(true);

        assertFalse(service.registrar(alerta("a2", "STOCK_AGOTADO", 0)));

        verifyNoInteractions(alertas);
        verify(procesados, never()).save(any());
    }

    @Test
    void rechazaTiposDesconocidosYCantidadesInvalidas() {
        assertThrows(EventoInvalidoException.class, () -> service.registrar(alerta("a3", "OTRA_COSA", 3)));
        assertThrows(EventoInvalidoException.class, () -> service.registrar(alerta("a4", "STOCK_BAJO", -1)));
        assertThrows(EventoInvalidoException.class, () -> service.registrar(alerta("a5", "STOCK_BAJO", null)));
        assertThrows(EventoInvalidoException.class, () -> service.registrar(alerta(" ", "STOCK_BAJO", 3)));
        assertThrows(EventoInvalidoException.class, () -> service.registrar(null));
        verifyNoInteractions(alertas);
    }
}
