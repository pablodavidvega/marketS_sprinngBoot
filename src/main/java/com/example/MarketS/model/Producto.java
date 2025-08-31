package com.example.MarketS.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "producto")
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
    private Integer stock;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
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

    @ManyToOne
    @JoinColumn(name = "vendedor_id")
    private User vendedor;
}
