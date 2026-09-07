package cl.duoc.pedidos360.ms_clientes.repository;

import cl.duoc.pedidos360.ms_clientes.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByEmail(String email);
}