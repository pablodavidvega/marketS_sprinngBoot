package com.example.MarketS.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;


@Entity
@Table(name = "pedidos", indexes = {
        @Index(name = "idx_pedidos_id_usuario", columnList = "id_usuario"),
        @Index(name = "idx_pedidos_estado_pedido", columnList = "estado_pedido")
})
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long id;

    // CORREGIDO: Agregar orphanRemoval y fetch type
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DetallePedido> detalles = new ArrayList<>();

    // LAZY: igual que Producto.vendedor, cargar el User completo del comprador dispara una
    // consulta extra a "vendedores" (OneToOne mappedBy sin bytecode enhancement). En listados
    // con muchos compradores distintos (ver PedidoRepository.findByVendedorWithDetails) el
    // username/email se resuelve aparte en batch en vez de navegar pedido.getUsuario().
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private User usuario;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "total", nullable = false)
    private Double total;

    @Column(name = "estado_pedido", nullable = false)
    private String estado;

    // Método de pago elegido al crear el pedido: MERCADOPAGO (default) o un acuerdo externo
    // (EFECTIVO, TRANSFERENCIA, NEQUI, OTRO). Determina si el pedido pasa por el flujo de
    // MercadoPago o queda a la espera de que el vendedor confirme manualmente la venta.
    @Column(name = "metodo_pago", length = 30)
    private String metodoPago = "MERCADOPAGO";

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

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }
}
