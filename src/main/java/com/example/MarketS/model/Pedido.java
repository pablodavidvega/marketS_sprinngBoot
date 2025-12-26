package com.example.MarketS.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;


@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long id;

    // CORREGIDO: Agregar orphanRemoval y fetch type
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DetallePedido> detalles = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private User usuario;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "total", nullable = false)
    private Double total;

    @Column(name = "estado_pedido", nullable = false)
    private String estado;

    // ===== CONSTRUCTORES =====
    public Pedido() {
        this.detalles = new ArrayList<>(); // INICIALIZAR SIEMPRE
        this.fecha = LocalDateTime.now();
    }

    // Constructor actualizado
    public Pedido(User usuario, Double total, String estado) {
        this();
        this.usuario = usuario;
        this.total = total;
        this.estado = estado;
    }

    // ===== MÉTODO HELPER PARA AGREGAR DETALLES =====
    public void agregarDetalle(DetallePedido detalle) {
        detalle.setPedido(this);
        this.detalles.add(detalle);
    }

    // ===== GETTERS Y SETTERS =====

    public List<DetallePedido> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetallePedido> detalles) {
        this.detalles = detalles;
    }


    public Long getId() {
        return id;
    }

    public User getUsuario() {
        return usuario;
    }

    public void setUsuario(User usuario) {
        this.usuario = usuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
