package cl.duoc.pedidos360.ms_clientes.service;

import cl.duoc.pedidos360.ms_clientes.model.Cliente;
import cl.duoc.pedidos360.ms_clientes.repository.ClienteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {
    private final ClienteRepository repository;

    public List<Cliente> listar() {
        return repository.findAll();
    }

    public Cliente obtener(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));
    }

    public Cliente crear(Cliente cliente) {
        return repository.save(cliente);
    }

    public Cliente actualizar(Long id, Cliente datos) {
        Cliente existente = obtener(id);
        existente.setNombre(datos.getNombre());
        existente.setEmail(datos.getEmail());
        existente.setTelefono(datos.getTelefono());
        existente.setDireccion(datos.getDireccion());
        return repository.save(existente);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}