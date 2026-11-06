package cl.duoc.pedidos360.ms_productos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "productos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser mayor a 0")
    private BigDecimal precio;

    @NotBlank(message = "La categoría es obligatoria")
    private String categoria;

    // Oferta: precio habitual antes del descuento. El que se cobra siempre es "precio".
    @Positive(message = "El precio anterior debe ser mayor a 0")
    @Column(name = "precio_anterior")
    private BigDecimal precioAnterior;

    // Foto del producto (URL). Si va vacía, el frontend busca assets/productos/{id}.jpg.
    private String imagenUrl;

    private boolean activo = true;
}
