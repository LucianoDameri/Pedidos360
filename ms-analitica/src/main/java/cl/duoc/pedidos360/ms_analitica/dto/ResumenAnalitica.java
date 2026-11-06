package cl.duoc.pedidos360.ms_analitica.dto;

import java.math.BigDecimal;

/** Respuesta de GET /analitica/resumen (mensaje de SALIDA de este servicio). */
public record ResumenAnalitica(
        long pedidosCreados,
        long pedidosConfirmados,
        long pedidosCancelados,
        long unidadesVendidas,
        BigDecimal ingresos,
        long alertasStock,
        long productosConAlerta) {
}
