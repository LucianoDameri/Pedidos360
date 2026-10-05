package cl.duoc.pedidos360.ms_pagos.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Llama a ms-pedidos reenviando el JWT del usuario. */
@Component
public class ServiciosClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA =
            new ParameterizedTypeReference<>() {};

    private final RestClient rest;
    private final String pedidosUrl;

    public ServiciosClient(RestClient rest, @Value("${servicios.pedidos-url}") String pedidosUrl) {
        this.rest = rest;
        this.pedidosUrl = pedidosUrl;
    }

    /** Devuelve null si el pedido no existe. */
    public Map<String, Object> obtenerPedido(Long id, String auth) {
        return obtener(pedidosUrl + "/pedidos/" + id, auth, "pedidos");
    }

    /** Marca el pedido como CONFIRMADO una vez aprobado el pago. */
    public void confirmarPedido(Long id, String auth) {
        try {
            rest.patch()
                    .uri(pedidosUrl + "/pedidos/" + id + "/estado?estado=CONFIRMADO")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()),
                    "No se pudo confirmar el pedido (HTTP " + e.getStatusCode().value() + ")");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "El servicio de pedidos no está disponible");
        }
    }

    private Map<String, Object> obtener(String url, String auth, String servicio) {
        try {
            return rest.get().uri(url)
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve()
                    .body(MAPA);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()),
                    "No se pudo consultar " + servicio + " (HTTP " + e.getStatusCode().value() + ")");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "El servicio de " + servicio + " no está disponible");
        }
    }
}
