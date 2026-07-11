package com.example.MarketS.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion", indexes = {
        @Index(name = "idx_notificacion_usuario_leida", columnList = "usuario_id, leida"),
        @Index(name = "idx_notificacion_usuario_fecha", columnList = "usuario_id, fecha_creacion")
})
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Column(nullable = false)
    private String mensaje;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "producto_id")
    private Long productoId;

    // Solo se usa para hilos de chat/encuentro (un pedido puede tener varios vendedores),
    // así el frontend sabe cuál hilo abrir. Null para el resto de tipos de notificación.
    @Column(name = "vendedor_id")
    private Long vendedorId;
}
