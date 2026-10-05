package cl.duoc.pedidos360.ms_pedidos.client;

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

/**
 * Llama a clientes, productos e inventario reenviando el JWT del usuario
 * (cada microservicio vuelve a validarlo).
 */
@Component
public class ServiciosClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA =
            new ParameterizedTypeReference<>() {};

    private final RestClient rest;
    private final String clientesUrl;
    private final String productosUrl;
    private final String inventarioUrl;

    public ServiciosClient(RestClient rest,
                           @Value("${servicios.clientes-url}") String clientesUrl,
                           @Value("${servicios.productos-url}") String productosUrl,
                           @Value("${servicios.inventario-url}") String inventarioUrl) {
        this.rest = rest;
        this.clientesUrl = clientesUrl;
        this.productosUrl = productosUrl;
        this.inventarioUrl = inventarioUrl;
    }

    /** Devuelve null si el cliente no existe. */
    public Map<String, Object> obtenerCliente(Long id, String auth) {
        return obtener(clientesUrl + "/clientes/" + id, auth, "clientes");
    }

    /** Devuelve null si el producto no existe. */
    public Map<String, Object> obtenerProducto(Long id, String auth) {
        return obtener(productosUrl + "/productos/" + id, auth, "productos");
    }

    /** delta negativo descuenta stock; positivo lo repone. */
    public void ajustarStock(Long productoId, int delta, String auth) {
        try {
            rest.patch()
                    .uri(inventarioUrl + "/inventario/producto/" + productoId + "/ajuste?delta=" + delta)
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El producto no tiene stock registrado en inventario");
        } catch (HttpClientErrorException.Conflict e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente para el producto");
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode().value()),
                    "No se pudo actualizar el inventario (HTTP " + e.getStatusCode().value() + ")");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "El servicio de inventario no está disponible");
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
