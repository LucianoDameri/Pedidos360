package cl.duoc.pedidos360.ms_analitica.service;

import cl.duoc.pedidos360.ms_analitica.exception.EventoInvalidoException;
import cl.duoc.pedidos360.ms_analitica.messaging.dto.PedidoEventoDto;
import cl.duoc.pedidos360.ms_analitica.model.EventoProcesado;
import cl.duoc.pedidos360.ms_analitica.model.VentaProducto;
import cl.duoc.pedidos360.ms_analitica.repository.EventoProcesadoRepository;
import cl.duoc.pedidos360.ms_analitica.repository.VentaProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentasServiceTest {

    @Mock
    private VentaProductoRepository ventas;
    @Mock
    private EventoProcesadoRepository procesados;
    @InjectMocks
    private VentasService service;

    private PedidoEventoDto evento(String id, String tipo, Integer cantidad, String total) {
        return new PedidoEventoDto(id, tipo, 1, 12L, 3L, 7L, cantidad,
                total != null ? new BigDecimal(total) : null, "ESTADO", "2026-11-02T14:35:10");
    }

    @Test
    void unPedidoCreadoSumaUnPedidoCreadoAlProducto() {
        when(procesados.existsById("e1")).thenReturn(false);
        when(ventas.buscarParaActualizar(7L)).thenReturn(Optional.empty());

        boolean afecto = service.registrar(evento("e1", "PEDIDO_CREADO", 2, "2000"));

        assertTrue(afecto);
        ArgumentCaptor<VentaProducto> guardada = ArgumentCaptor.forClass(VentaProducto.class);
        verify(ventas).save(guardada.capture());
        assertEquals(7L, guardada.getValue().getProductoId());
        assertEquals(1, guardada.getValue().getPedidosCreados());
        assertEquals(0, guardada.getValue().getUnidadesVendidas());
        verify(procesados).save(any(EventoProcesado.class));
    }

    @Test
    void unPedidoConfirmadoSumaUnidadesEIngresos() {
        VentaProducto existente = new VentaProducto(7L);
        existente.setUnidadesVendidas(3);
        existente.setPedidosConfirmados(1);
        existente.setIngresos(new BigDecimal("3000.00"));
        when(procesados.existsById("e2")).thenReturn(false);
        when(ventas.buscarParaActualizar(7L)).thenReturn(Optional.of(existente));

        service.registrar(evento("e2", "PEDIDO_CONFIRMADO", 2, "2000"));

        assertEquals(5, existente.getUnidadesVendidas());
        assertEquals(2, existente.getPedidosConfirmados());
        assertEquals(0, new BigDecimal("5000").compareTo(existente.getIngresos()));
        verify(ventas).save(existente);
    }

    @Test
    void unPedidoCanceladoSumaUnPedidoCancelado() {
        VentaProducto existente = new VentaProducto(7L);
        when(procesados.existsById("e3")).thenReturn(false);
        when(ventas.buscarParaActualizar(7L)).thenReturn(Optional.of(existente));

        service.registrar(evento("e3", "PEDIDO_CANCELADO", 2, "2000"));

        assertEquals(1, existente.getPedidosCancelados());
        assertEquals(0, existente.getUnidadesVendidas());
    }

    @Test
    void losEstadosDeLogisticaNoCambianLasMetricasPeroQuedanRegistrados() {
        when(procesados.existsById("e4")).thenReturn(false);
        when(ventas.buscarParaActualizar(7L)).thenReturn(Optional.of(new VentaProducto(7L)));

        boolean afecto = service.registrar(evento("e4", "PEDIDO_EN_CAMINO", 2, "2000"));

        assertFalse(afecto);
        verify(ventas, never()).save(any());
        verify(procesados).save(any(EventoProcesado.class));
    }

    @Test
    void unEventoDuplicadoSeIgnora() {
        when(procesados.existsById("e5")).thenReturn(true);

        boolean afecto = service.registrar(evento("e5", "PEDIDO_CONFIRMADO", 2, "2000"));

        assertFalse(afecto);
        verifyNoInteractions(ventas);
        verify(procesados, never()).save(any());
    }

    @Test
    void unMensajeSinContenidoEsInvalido() {
        assertThrows(EventoInvalidoException.class, () -> service.registrar(null));
    }

    @Test
    void unEventoSinEventIdOSinTipoOSinProductoEsInvalido() {
        assertThrows(EventoInvalidoException.class, () -> service.registrar(evento(null, "PEDIDO_CREADO", 1, "10")));
        assertThrows(EventoInvalidoException.class, () -> service.registrar(evento("x", " ", 1, "10")));
        PedidoEventoDto sinProducto = new PedidoEventoDto("x", "PEDIDO_CREADO", 1, 12L, 3L, null, 1,
                BigDecimal.TEN, "PENDIENTE", null);
        assertThrows(EventoInvalidoException.class, () -> service.registrar(sinProducto));
    }

    @Test
    void unPedidoConfirmadoConCantidadOTotalInvalidosEsInvalido() {
        assertThrows(EventoInvalidoException.class, () -> service.registrar(evento("a", "PEDIDO_CONFIRMADO", 0, "10")));
        assertThrows(EventoInvalidoException.class, () -> service.registrar(evento("b", "PEDIDO_CONFIRMADO", null, "10")));
        assertThrows(EventoInvalidoException.class, () -> service.registrar(evento("c", "PEDIDO_CONFIRMADO", 1, null)));
        assertThrows(EventoInvalidoException.class, () -> service.registrar(evento("d", "PEDIDO_CONFIRMADO", 1, "-5")));
        verifyNoInteractions(ventas);
    }

    @Test
    void unEventoInvalidoEsUnaIllegalArgumentExceptionParaQueVayaDirectoALaDlt() {
        EventoInvalidoException error = new EventoInvalidoException("malo");
        assertTrue(error instanceof IllegalArgumentException);
    }
}
