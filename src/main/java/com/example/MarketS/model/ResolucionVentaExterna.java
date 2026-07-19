package com.example.MarketS.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Un pedido puede incluir productos de más de un vendedor (el carrito no se separa por
// vendedor antes de pagar). Cuando el pago es externo (efectivo/transferencia/etc.), cada
// vendedor confirma o rechaza SU parte de forma independiente — esta tabla guarda esa
// resolución por (pedido, vendedor) en vez de depender de Pedido.estado, que es un solo
// campo compartido y no puede representar que el vendedor A ya confirmó mientras el
// vendedor B todavía no actuó (o decidió lo contrario).
@Entity
@Table(name = "resoluciones_venta_externa",
       uniqueConstraints = @UniqueConstraint(columnNames = {"pedido_id", "vendedor_id"}))
public class ResolucionVentaExterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private User vendedor;

    @Column(nullable = false)
    private boolean completado;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public ResolucionVentaExterna() {}

    public ResolucionVentaExterna(Pedido pedido, User vendedor, boolean completado) {
        this.pedido = pedido;
        this.vendedor = vendedor;
        this.completado = completado;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public User getVendedor() { return vendedor; }
    public void setVendedor(User vendedor) { this.vendedor = vendedor; }

    public boolean isCompletado() { return completado; }
    public void setCompletado(boolean completado) { this.completado = completado; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
