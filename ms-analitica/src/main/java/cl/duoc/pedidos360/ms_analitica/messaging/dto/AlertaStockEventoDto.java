package cl.duoc.pedidos360.ms_analitica.messaging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Mensaje de ENTRADA del topico "inventario.alertas" (lo publica ms-inventario). Clave: productoId.
 *
 * tipo: STOCK_BAJO (cantidad <= stockMinimo) | STOCK_AGOTADO (cantidad = 0)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertaStockEventoDto(
        String eventId,
        String tipo,
        int version,
        Long productoId,
        Integer cantidadActual,
        Integer stockMinimo,
        String ocurridoEn) {

    public static final String STOCK_BAJO = "STOCK_BAJO";
    public static final String STOCK_AGOTADO = "STOCK_AGOTADO";
}
