package cl.duoc.pedidos360.ms_inventario.service;

import cl.duoc.pedidos360.ms_inventario.messaging.publisher.AlertaStockKafkaPublisher;
import cl.duoc.pedidos360.ms_inventario.model.Stock;
import cl.duoc.pedidos360.ms_inventario.repository.StockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** La alerta a Kafka solo sale cuando el stock BAJA y queda en el minimo o menos. */
@ExtendWith(MockitoExtension.class)
class StockServiceAlertaTest {

    @Mock
    private StockRepository repository;
    @Mock
    private AlertaStockKafkaPublisher alertas;
    @InjectMocks
    private StockService service;

    private Stock stock(int cantidad, int minimo) {
        Stock s = new Stock();
        s.setId(1L);
        s.setProductoId(7L);
        s.setCantidad(cantidad);
        s.setStockMinimo(minimo);
        return s;
    }

    @Test
    void publicaAlertaCuandoElStockBajaHastaElMinimo() {
        Stock existente = stock(8, 5);
        when(repository.findByProductoId(7L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        Stock resultado = service.ajustarCantidad(7L, -3);

        assertEquals(5, resultado.getCantidad());
        verify(alertas).publicar(existente);
    }

    @Test
    void publicaAlertaCuandoElProductoSeAgota() {
        Stock existente = stock(3, 5);
        when(repository.findByProductoId(7L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        service.ajustarCantidad(7L, -3);

        verify(alertas).publicar(existente);
    }

    @Test
    void noPublicaSiElStockSigueSobreElMinimo() {
        Stock existente = stock(20, 5);
        when(repository.findByProductoId(7L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        service.ajustarCantidad(7L, -3);

        verifyNoInteractions(alertas);
    }

    @Test
    void unaReposicionNoGeneraAlertaAunqueElStockSigaBajo() {
        Stock existente = stock(1, 5);
        when(repository.findByProductoId(7L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        service.ajustarCantidad(7L, 2);

        verifyNoInteractions(alertas);
    }

    @Test
    void sinStockSuficienteNoSeGuardaNiSePublica() {
        Stock existente = stock(2, 5);
        when(repository.findByProductoId(7L)).thenReturn(Optional.of(existente));

        assertThrows(IllegalStateException.class, () -> service.ajustarCantidad(7L, -3));

        verify(repository, never()).save(any());
        verifyNoInteractions(alertas);
    }
}
