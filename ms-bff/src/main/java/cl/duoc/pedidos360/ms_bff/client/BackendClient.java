package cl.duoc.pedidos360.ms_bff.client;

import cl.duoc.pedidos360.ms_bff.config.BackendProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;

/**
 * Cliente HTTP hacia los microservicios. Reenvía el JWT original del usuario
 * (token propagation) para que cada microservicio valide también el token.
 */
@Component
public class BackendClient {

    private final RestClient rest;
    private final BackendProperties props;

    public BackendClient(RestClient rest, BackendProperties props) {
        this.rest = rest;
        this.props = props;
    }

    public boolean exists(String service) {
        return props.getServices().containsKey(service);
    }

    /** Reenvía la petición tal cual y devuelve la respuesta (incluidos 4xx/5xx) sin transformarla. */
    public ResponseEntity<byte[]> forward(String service, HttpMethod method, String pathAndQuery,
                                          String authorization, MediaType contentType, byte[] body) {
        RestClient.RequestBodySpec spec = rest.method(method)
                .uri(URI.create(props.getServices().get(service) + pathAndQuery))
                .headers(h -> {
                    if (authorization != null) {
                        h.set(HttpHeaders.AUTHORIZATION, authorization);
                    }
                    if (contentType != null) {
                        h.setContentType(contentType);
                    }
                    h.setAccept(java.util.List.of(MediaType.APPLICATION_JSON, MediaType.ALL));
                });

        if (body != null && body.length > 0) {
            spec = spec.body(body);
        }

        return spec.exchange((request, response) -> {
            byte[] payload = response.getBody().readAllBytes();
            HttpHeaders headers = new HttpHeaders();
            if (response.getHeaders().getContentType() != null) {
                headers.setContentType(response.getHeaders().getContentType());
            }
            return ResponseEntity.status(response.getStatusCode()).headers(headers).body(payload);
        });
    }

    /** GET que deserializa el JSON a un Map/List genérico. Lanza excepción si el microservicio responde error. */
    public Object getJson(String service, String path, String authorization) {
        return rest.get()
                .uri(URI.create(props.getServices().get(service) + path))
                .headers(h -> {
                    if (authorization != null) {
                        h.set(HttpHeaders.AUTHORIZATION, authorization);
                    }
                })
                .retrieve()
                .body(Object.class);
    }
}
