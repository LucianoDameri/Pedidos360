package cl.duoc.pedidos360.ms_analitica.controller;

import cl.duoc.pedidos360.ms_analitica.dto.ResumenAnalitica;
import cl.duoc.pedidos360.ms_analitica.model.AlertaStock;
import cl.duoc.pedidos360.ms_analitica.model.VentaProducto;
import cl.duoc.pedidos360.ms_analitica.service.AnaliticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de lectura de las metricas calculadas a partir de los topicos Kafka.
 *
 *   GET /analitica/resumen                totales de ventas y alertas
 *   GET /analitica/ventas/productos       ventas por producto (mayor a menor)
 *   GET /analitica/ventas/top?limite=5    productos mas vendidos
 *   GET /analitica/alertas?limite=20      ultimas alertas de stock recibidas
 */
@RestController
@RequestMapping("/analitica")
@RequiredArgsConstructor
public class AnaliticaController {

    private final AnaliticaService service;

    @GetMapping("/resumen")
    public ResumenAnalitica resumen() {
        return service.resumen();
    }

    @GetMapping("/ventas/productos")
    public List<VentaProducto> ventasPorProducto() {
        return service.ventasPorProducto();
    }

    @GetMapping("/ventas/top")
    public List<VentaProducto> masVendidos(@RequestParam(defaultValue = "5") int limite) {
        return service.masVendidos(limite);
    }

    @GetMapping("/alertas")
    public List<AlertaStock> alertas(@RequestParam(defaultValue = "20") int limite) {
        return service.alertasRecientes(limite);
    }
}
