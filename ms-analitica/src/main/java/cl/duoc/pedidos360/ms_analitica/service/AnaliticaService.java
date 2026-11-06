package cl.duoc.pedidos360.ms_analitica.service;

import cl.duoc.pedidos360.ms_analitica.dto.ResumenAnalitica;
import cl.duoc.pedidos360.ms_analitica.model.AlertaStock;
import cl.duoc.pedidos360.ms_analitica.model.VentaProducto;
import cl.duoc.pedidos360.ms_analitica.repository.AlertaStockRepository;
import cl.duoc.pedidos360.ms_analitica.repository.VentaProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/** Consultas de solo lectura sobre las metricas que alimentan los consumidores Kafka. */
@Service
@RequiredArgsConstructor
public class AnaliticaService {

    private static final int LIMITE_MAXIMO = 100;

    private final VentaProductoRepository ventas;
    private final AlertaStockRepository alertas;

    public ResumenAnalitica resumen() {
        List<VentaProducto> todas = ventas.findAll();
        return new ResumenAnalitica(
                todas.stream().mapToLong(VentaProducto::getPedidosCreados).sum(),
                todas.stream().mapToLong(VentaProducto::getPedidosConfirmados).sum(),
                todas.stream().mapToLong(VentaProducto::getPedidosCancelados).sum(),
                todas.stream().mapToLong(VentaProducto::getUnidadesVendidas).sum(),
                todas.stream().map(VentaProducto::getIngresos).reduce(BigDecimal.ZERO, BigDecimal::add),
                alertas.count(),
                alertas.contarProductosConAlerta());
    }

    public List<VentaProducto> ventasPorProducto() {
        return ventas.findAllByOrderByUnidadesVendidasDescProductoIdAsc();
    }

    public List<VentaProducto> masVendidos(int limite) {
        return ventas.findAllByOrderByUnidadesVendidasDescProductoIdAsc(PageRequest.of(0, acotar(limite)));
    }

    public List<AlertaStock> alertasRecientes(int limite) {
        return alertas.findAllByOrderByRecibidoEnDescIdDesc(PageRequest.of(0, acotar(limite)));
    }

    private static int acotar(int limite) {
        return Math.max(1, Math.min(limite, LIMITE_MAXIMO));
    }
}
