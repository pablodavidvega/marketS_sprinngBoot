package com.example.MarketS.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class TallaStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String talla; // Ejemplo: S, M, L, XL
    private int stock;    // Cantidad disponible

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    // Getters y setters
}

