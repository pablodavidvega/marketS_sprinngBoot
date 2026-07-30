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

    // "COMPRADOR" o "VENDEDOR": en qué rol actúa el destinatario para ESTA notificación
    // puntual. Necesario porque un mismo usuario puede ser vendedor y a la vez comprador de
    // otro pedido, y tipos como NUEVO_MENSAJE_CHAT/ENCUENTRO_* se mandan indistintamente a
    // cualquiera de las dos partes de un mismo hilo — el tipo solo no alcanza para que el
    // frontend sepa a qué panel (/cliente/mis-pedidos vs /vendedor/pedidos) llevar al usuario.
    // Null para tipos que ya son inequívocos por sí mismos (p.ej. PEDIDO_CREADO siempre es al
    // comprador, NUEVO_PEDIDO siempre al vendedor) o para notificaciones previas a este campo.
    @Column(name = "rol_destino")
    private String rolDestino;
}
