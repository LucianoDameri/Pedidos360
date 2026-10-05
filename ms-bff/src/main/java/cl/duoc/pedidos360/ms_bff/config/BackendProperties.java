package cl.duoc.pedidos360.ms_bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/** URLs base de los microservicios (bff.services.*). */
@ConfigurationProperties(prefix = "bff")
public class BackendProperties {

    private Map<String, String> services = new HashMap<>();

    public Map<String, String> getServices() {
        return services;
    }

    public void setServices(Map<String, String> services) {
        this.services = services;
    }
}
