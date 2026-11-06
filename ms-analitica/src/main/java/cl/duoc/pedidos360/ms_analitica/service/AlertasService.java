package cl.duoc.pedidos360.ms_analitica.service;

import cl.duoc.pedidos360.ms_analitica.exception.EventoInvalidoException;
import cl.duoc.pedidos360.ms_analitica.messaging.dto.AlertaStockEventoDto;
import cl.duoc.pedidos360.ms_analitica.model.AlertaStock;
import cl.duoc.pedidos360.ms_analitica.model.EventoProcesado;
import cl.duoc.pedidos360.ms_analitica.repository.AlertaStockRepository;
import cl.duoc.pedidos360.ms_analitica.repository.EventoProcesadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Guarda el historial de alertas de stock para reposicion. No sabe nada de Kafka. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertasService {

    private static final String TOPICO = "inventario.alertas";

    private final AlertaStockRepository alertas;
    private final EventoProcesadoRepository procesados;

    /** @return true si se guardo la alerta; false si era un evento duplicado. */
    @Transactional
    public boolean registrar(AlertaStockEventoDto evento) {
        validar(evento);

        if (procesados.existsById(evento.eventId())) {
            log.info("[ANALITICA] Alerta duplicada ignorada (eventId={})", evento.eventId());
            return false;
        }

        AlertaStock alerta = new AlertaStock();
        alerta.setEventId(evento.eventId());
        alerta.setProductoId(evento.productoId());
        alerta.setTipo(evento.tipo());
        alerta.setCantidadActual(evento.cantidadActual());
        alerta.setStockMinimo(evento.stockMinimo());
        alerta.setOcurridoEn(evento.ocurridoEn());
        alerta.setRecibidoEn(LocalDateTime.now());
        alertas.save(alerta);
        procesados.save(new EventoProcesado(evento.eventId(), TOPICO, LocalDateTime.now()));

        log.warn("[ANALITICA] Reposicion sugerida: producto {} con {} unidades (minimo {}) - {}",
                evento.productoId(), evento.cantidadActual(), evento.stockMinimo(), evento.tipo());
        return true;
    }

    private void validar(AlertaStockEventoDto e) {
        if (e == null) {
            throw new EventoInvalidoException("El mensaje no trae contenido");
        }
        if (e.eventId() == null || e.eventId().isBlank()) {
            throw new EventoInvalidoException("La alerta no trae eventId");
        }
        if (e.productoId() == null) {
            throw new EventoInvalidoException("La alerta no trae productoId");
        }
        if (!AlertaStockEventoDto.STOCK_BAJO.equals(e.tipo()) && !AlertaStockEventoDto.STOCK_AGOTADO.equals(e.tipo())) {
            throw new EventoInvalidoException("Tipo de alerta desconocido: " + e.tipo());
        }
        if (e.cantidadActual() == null || e.cantidadActual() < 0) {
            throw new EventoInvalidoException("La alerta trae una cantidad invalida: " + e.cantidadActual());
        }
    }
}
