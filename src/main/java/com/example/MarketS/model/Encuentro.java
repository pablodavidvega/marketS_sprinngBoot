package com.example.MarketS.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Un encuentro acordado por pedido+vendedor (mismo criterio de hilo que ChatMensaje, ya que un
// pedido puede tener varios vendedores). A diferencia del chat (histórico de mensajes), acá se
// guarda un único registro "vivo" por hilo: proponer/editar hace upsert, cancelar lo borra.
@Entity
@Table(name = "encuentros",
       uniqueConstraints = @UniqueConstraint(columnNames = {"pedido_id", "vendedor_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Encuentro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private User vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propuesto_por_id", nullable = false)
    private User propuestoPor;

    @Column(nullable = false)
    private String lugar;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(columnDefinition = "TEXT")
    private String nota;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
