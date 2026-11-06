package cl.duoc.pedidos360.ms_analitica.service;

import cl.duoc.pedidos360.ms_analitica.dto.ResumenAnalitica;
import cl.duoc.pedidos360.ms_analitica.model.VentaProducto;
import cl.duoc.pedidos360.ms_analitica.repository.AlertaStockRepository;
import cl.duoc.pedidos360.ms_analitica.repository.VentaProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnaliticaServiceTest {

    @Mock
    private VentaProductoRepository ventas;
    @Mock
    private AlertaStockRepository alertas;
    @InjectMocks
    private AnaliticaService service;

    private VentaProducto venta(long productoId, long creados, long confirmados, long cancelados, long unidades, String ingresos) {
        VentaProducto v = new VentaProducto(productoId);
        v.setPedidosCreados(creados);
        v.setPedidosConfirmados(confirmados);
        v.setPedidosCancelados(cancelados);
        v.setUnidadesVendidas(unidades);
        v.setIngresos(new BigDecimal(ingresos));
        return v;
    }

    @Test
    void elResumenSumaLasMetricasDeTodosLosProductos() {
        when(ventas.findAll()).thenReturn(List.of(
                venta(7L, 5, 3, 1, 6, "6000.00"),
                venta(8L, 2, 2, 0, 2, "1500.50")));
        when(alertas.count()).thenReturn(4L);
        when(alertas.contarProductosConAlerta()).thenReturn(2L);

        ResumenAnalitica resumen = service.resumen();

        assertEquals(7, resumen.pedidosCreados());
        assertEquals(5, resumen.pedidosConfirmados());
        assertEquals(1, resumen.pedidosCancelados());
        assertEquals(8, resumen.unidadesVendidas());
        assertEquals(0, new BigDecimal("7500.50").compareTo(resumen.ingresos()));
        assertEquals(4, resumen.alertasStock());
        assertEquals(2, resumen.productosConAlerta());
    }

    @Test
    void elLimiteDeLasConsultasSeAcotaEntre1Y100() {
        when(ventas.findAllByOrderByUnidadesVendidasDescProductoIdAsc(any(Pageable.class))).thenReturn(List.of());
        when(alertas.findAllByOrderByRecibidoEnDescIdDesc(any(Pageable.class))).thenReturn(List.of());

        service.masVendidos(1000);
        service.masVendidos(0);
        service.alertasRecientes(500);

        ArgumentCaptor<Pageable> ventasPagina = ArgumentCaptor.forClass(Pageable.class);
        verify(ventas, org.mockito.Mockito.times(2)).findAllByOrderByUnidadesVendidasDescProductoIdAsc(ventasPagina.capture());
        assertEquals(100, ventasPagina.getAllValues().get(0).getPageSize());
        assertEquals(1, ventasPagina.getAllValues().get(1).getPageSize());

        ArgumentCaptor<Pageable> alertasPagina = ArgumentCaptor.forClass(Pageable.class);
        verify(alertas).findAllByOrderByRecibidoEnDescIdDesc(alertasPagina.capture());
        assertEquals(100, alertasPagina.getValue().getPageSize());
    }
}
