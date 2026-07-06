package com.example.MarketS.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "producto", indexes = {
        @Index(name = "idx_producto_vendedor_id", columnList = "vendedor_id")
})
@Getter
@Setter
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private Double precio;
    @Temporal(TemporalType.DATE)
    @Column(name = "fecha_agregacion")
    private Date fechaAgregacion;
    private String estadoProducto;
    private String descripcion;
    private String imagen;
    private String imagen2;
    private String imagen3;
    private String video;
    private Integer stock;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TallaStock> tallas = new ArrayList<>();


    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public boolean getActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }


    @Override
    public String toString() {
        return "Producto{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", precio=" + precio +
                ", fechaAgregacion=" + fechaAgregacion +
                ", estadoProducto='" + estadoProducto + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", imagen='" + imagen + '\'' +
                ", marca=" + marca +
                ", categorias=" + categorias +
                '}';
    }

    @ManyToOne
    private Marca marca;

    @ManyToOne
    private Categorias categorias;

    // LAZY: el id del vendedor se puede leer desde el proxy sin consultar la BD (la FK ya
    // está en esta misma fila). Cargar el User completo (.getUsername(), etc.) sí dispara una
    // consulta — y como User.vendedor es un @OneToOne mappedBy sin bytecode enhancement,
    // Hibernate SIEMPRE dispara una consulta extra a "vendedores" al inicializar ese User,
    // se acceda o no a ese campo. Por eso el listado de productos usa un batch aparte
    // (ver ProductoApiController.buildDTOs) en vez de cargar el User completo por producto.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id")
    private User vendedor;
}
