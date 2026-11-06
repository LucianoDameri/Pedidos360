package cl.duoc.pedidos360.ms_analitica.service;

import cl.duoc.pedidos360.ms_analitica.exception.EventoInvalidoException;
import cl.duoc.pedidos360.ms_analitica.messaging.dto.PedidoEventoDto;
import cl.duoc.pedidos360.ms_analitica.model.EventoProcesado;
import cl.duoc.pedidos360.ms_analitica.model.VentaProducto;
import cl.duoc.pedidos360.ms_analitica.repository.EventoProcesadoRepository;
import cl.duoc.pedidos360.ms_analitica.repository.VentaProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Logica de negocio de las metricas de venta. No sabe nada de Kafka:
 * recibe el DTO ya leido y lanza EventoInvalidoException si el contenido no sirve.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VentasService {

    private static final String TOPICO = "pedidos.eventos";

    private final VentaProductoRepository ventas;
    private final EventoProcesadoRepository procesados;

    /**
     * Actualiza las metricas del producto segun el tipo de evento.
     *  - PEDIDO_CREADO     -> cuenta un pedido creado.
     *  - PEDIDO_CONFIRMADO -> el pedido se pago: suma unidades e ingresos.
     *  - PEDIDO_CANCELADO  -> cuenta un pedido cancelado.
     *  - otros estados (preparacion, camino, entregado) no cambian las metricas.
     *
     * @return true si el evento modifico las metricas; false si era duplicado o no aplica.
     */
    @Transactional
    public boolean registrar(PedidoEventoDto evento) {
        validar(evento);

        if (procesados.existsById(evento.eventId())) {
            log.info("[ANALITICA] Evento duplicado ignorado (eventId={}, tipo={})", evento.eventId(), evento.tipo());
            return false;
        }

        boolean afectaMetricas = true;
        VentaProducto venta = ventas.buscarParaActualizar(evento.productoId())
                .orElseGet(() -> new VentaProducto(evento.productoId()));

        switch (evento.tipo()) {
            case PedidoEventoDto.CREADO -> venta.setPedidosCreados(venta.getPedidosCreados() + 1);
            case PedidoEventoDto.CONFIRMADO -> {
                venta.setPedidosConfirmados(venta.getPedidosConfirmados() + 1);
                venta.setUnidadesVendidas(venta.getUnidadesVendidas() + evento.cantidad());
                venta.setIngresos(venta.getIngresos().add(evento.total()));
            }
            case PedidoEventoDto.CANCELADO -> venta.setPedidosCancelados(venta.getPedidosCancelados() + 1);
            default -> afectaMetricas = false;
        }

        if (afectaMetricas) {
            venta.setActualizadoEn(LocalDateTime.now());
            ventas.save(venta);
        }
        procesados.save(new EventoProcesado(evento.eventId(), TOPICO, LocalDateTime.now()));
        log.info("[ANALITICA] Procesado {} (pedidoId={}, productoId={}){}", evento.tipo(), evento.pedidoId(),
                evento.productoId(), afectaMetricas ? "" : " sin cambios en las metricas");
        return afectaMetricas;
    }

    private void validar(PedidoEventoDto e) {
        if (e == null) {
            throw new EventoInvalidoException("El mensaje no trae contenido");
        }
        if (vacio(e.eventId())) {
            throw new EventoInvalidoException("El evento no trae eventId");
        }
        if (vacio(e.tipo())) {
            throw new EventoInvalidoException("El evento no trae tipo");
        }
        if (e.productoId() == null) {
            throw new EventoInvalidoException("El evento no trae productoId");
        }
        if (PedidoEventoDto.CONFIRMADO.equals(e.tipo())) {
            if (e.cantidad() == null || e.cantidad() <= 0) {
                throw new EventoInvalidoException("PEDIDO_CONFIRMADO con cantidad invalida: " + e.cantidad());
            }
            if (e.total() == null || e.total().compareTo(BigDecimal.ZERO) < 0) {
                throw new EventoInvalidoException("PEDIDO_CONFIRMADO con total invalido: " + e.total());
            }
        }
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
