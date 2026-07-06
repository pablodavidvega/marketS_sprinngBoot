package com.example.MarketS.model;

import jakarta.persistence.*;

@Entity
@Table(name = "detalle_pedido", indexes = {
        @Index(name = "idx_detalle_pedido_id_pedido", columnList = "id_pedido"),
        @Index(name = "idx_detalle_pedido_id_producto", columnList = "id_producto")
})
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false)
    private Double precioUnitario;

    @Column(name = "comision_app")
    private Double comisionApp;

    @Column(name = "ganancia_vendedor")
    private Double gananciaVendedor;

    public DetallePedido() {}

    public DetallePedido(Pedido pedido, Producto producto, Integer cantidad, Double precioUnitario) {
        this.pedido = pedido;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;

        if (pedido != null) {
            pedido.getDetalles().add(this);
        }
    }
// ===== GETTERS Y SETTERS =====

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public Double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(Double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public Double getComisionApp() {
        return comisionApp;
    }

    public void setComisionApp(Double comisionApp) {
        this.comisionApp = comisionApp;
    }

    public Double getGananciaVendedor() {
        return gananciaVendedor;
    }

    public void setGananciaVendedor(Double gananciaVendedor) {
        this.gananciaVendedor = gananciaVendedor;
    }
}
