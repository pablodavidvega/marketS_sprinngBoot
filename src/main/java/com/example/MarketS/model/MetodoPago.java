package com.example.MarketS.model;
import jakarta.persistence.*;

@Entity
@Table(name = "metodos_pago")
public class MetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_metodo")
    private Long id;

    // Ej: Tarjeta, Efectivo, Transferencia, Nequi, etc.
    @Column(nullable = false, length = 50)
    private String nombre;

    // Campo opcional: descripción
    @Column(length = 200)
    private String descripcion;

    // Activo o inactivo (por si luego deshabilitas uno)
    @Column(nullable = false)
    private Boolean activo = true;

    // Constructor vacío
    public MetodoPago() {}

    // Getters y setters...
}
