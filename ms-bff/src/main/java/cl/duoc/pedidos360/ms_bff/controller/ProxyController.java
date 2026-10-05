package cl.duoc.pedidos360.ms_bff.controller;

import cl.duoc.pedidos360.ms_bff.client.BackendClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone al frontend las mismas rutas de los 5 microservicios
 * (/clientes, /productos, /pedidos, /inventario, /pagos) y las consume
 * por detrás con el JWT del usuario. El frontend solo conoce al BFF.
 */
@RestController
public class ProxyController {

    private final BackendClient backend;

    public ProxyController(BackendClient backend) {
        this.backend = backend;
    }

    @RequestMapping({
            "/clientes", "/clientes/**",
            "/productos", "/productos/**",
            "/pedidos", "/pedidos/**",
            "/inventario", "/inventario/**",
            "/pagos", "/pagos/**"
    })
    public ResponseEntity<byte[]> proxy(HttpServletRequest request,
                                        @RequestBody(required = false) byte[] body) {
        String uri = request.getRequestURI();
        String service = uri.split("/")[1];

        if (!backend.exists(service)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String pathAndQuery = request.getQueryString() == null ? uri : uri + "?" + request.getQueryString();
        MediaType contentType = request.getContentType() == null
                ? null : MediaType.parseMediaType(request.getContentType());

        return backend.forward(service, HttpMethod.valueOf(request.getMethod()), pathAndQuery,
                request.getHeader(HttpHeaders.AUTHORIZATION), contentType, body);
    }
}
