package cl.duoc.pedidos360.ms_rabbit_admin.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitAdmin permite crear y eliminar colas, exchanges y bindings en tiempo
 * de ejecucion. Toda la configuracion de conexion (cluster, usuario, clave)
 * viene de application.yml.
 */
@Configuration
public class RabbitAdminConfig {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
