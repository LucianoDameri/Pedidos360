package cl.duoc.pedidos360.ms_productos.config;

import cl.duoc.pedidos360.ms_productos.model.Producto;
import cl.duoc.pedidos360.ms_productos.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Carga el catálogo inicial de la tienda al arrancar, solo si la tabla está vacía.
 * Así los productos existen aunque se levante el sistema con una base de datos nueva,
 * y no pisa nunca los productos que ya existan (ni los nombres editados desde la web).
 * Debe coincidir con docs/seed-catalogo.sql y con StockInicial de ms-inventario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogoInicial implements CommandLineRunner {

    private final ProductoRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        // Se guardan en este orden para que los ids sean 1..12, como espera el inventario inicial.
        List<Producto> productos = List.of(
                producto("Notebook Lenovo IdeaPad 15", "Intel Core i5, 16 GB RAM, SSD 512 GB", 549990, 649990, "Computación"),
                producto("Mouse inalámbrico Logitech M170", "Receptor USB, 12 meses de batería", 9990, null, "Accesorios"),
                producto("Teclado mecánico Redragon", "Switch rojo, retroiluminado, layout español", 34990, 44990, "Accesorios"),
                producto("Monitor Samsung 24\" Full HD", "Panel IPS, 75 Hz, HDMI", 119990, null, "Computación"),
                producto("Audífonos Sony WH-CH520", "Bluetooth, hasta 50 horas de batería", 39990, 49990, "Audio"),
                producto("Parlante JBL Flip 6", "Bluetooth, resistente al agua IP67", 99990, null, "Audio"),
                producto("Smartphone Samsung Galaxy A15", "128 GB, pantalla AMOLED 6.5\"", 169990, 199990, "Telefonía"),
                producto("Cargador rápido 25W USB-C", "Compatible con Android y iPhone", 12990, null, "Telefonía"),
                producto("Webcam Logitech C920", "Full HD 1080p con micrófono estéreo", 59990, 74990, "Computación"),
                producto("Disco SSD Kingston 1 TB", "NVMe M.2, lectura hasta 3500 MB/s", 64990, null, "Computación"),
                producto("Tablet Lenovo Tab M10", "10.1\", 4 GB RAM, 64 GB", 139990, null, "Telefonía"),
                producto("Silla gamer ergonómica", "Reclinable, soporte lumbar, hasta 120 kg", 149990, 189990, "Muebles")
        );
        for (Producto p : productos) {
            repository.save(p);
        }
        log.info("[CATALOGO] Base de datos vacía: se cargaron {} productos iniciales", productos.size());
    }

    private Producto producto(String nombre, String descripcion, int precio, Integer precioAnterior, String categoria) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(BigDecimal.valueOf(precio));
        p.setPrecioAnterior(precioAnterior == null ? null : BigDecimal.valueOf(precioAnterior));
        p.setCategoria(categoria);
        p.setActivo(true);
        return p;
    }
}
