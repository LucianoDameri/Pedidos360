package cl.duoc.pedidos360.ms_bff.controller;

import cl.duoc.pedidos360.ms_bff.client.BackendClient;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Endpoints propios del BFF (composición de varias APIs en una sola llamada). */
@RestController
@RequestMapping("/bff")
public class BffController {

    private final BackendClient backend;

    public BffController(BackendClient backend) {
        this.backend = backend;
    }

    /** Claims del JWT ya validado: sirve para comprobar que el BFF reconoce al usuario. */
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> me = new LinkedHashMap<>();
        me.put("sub", jwt.getSubject());
        me.put("name", jwt.getClaimAsString("name"));
        me.put("usuario", firstNonNull(jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("upn"), jwt.getClaimAsString("email")));
        me.put("scopes", jwt.getClaimAsString("scp"));
        me.put("roles", jwt.getClaim("roles"));
        me.put("expira", jwt.getExpiresAt());
        return me;
    }

    /**
     * Resumen de un pedido: junta pedido + cliente + producto + stock + pagos
     * (5 microservicios) en una sola respuesta para el frontend.
     * Si el pedido no existe responde 404; si falta algún dato relacionado, queda en null.
     */
    @GetMapping("/pedidos/{id}/resumen")
    public Map<String, Object> resumenPedido(@PathVariable Long id,
                                             @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        Object pedido = backend.getJson("pedidos", "/pedidos/" + id, auth);

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("pedido", pedido);

        if (pedido instanceof Map<?, ?> p) {
            Object clienteId = p.get("clienteId");
            Object productoId = p.get("productoId");
            resumen.put("cliente", safeGet("clientes", "/clientes/" + clienteId, auth));
            resumen.put("producto", safeGet("productos", "/productos/" + productoId, auth));
            resumen.put("stock", safeGet("inventario", "/inventario/producto/" + productoId, auth));
        }
        resumen.put("pagos", safeGet("pagos", "/pagos/pedido/" + id, auth));
        return resumen;
    }

    private Object safeGet(String service, String path, String auth) {
        try {
            return backend.getJson(service, path, auth);
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }
}
