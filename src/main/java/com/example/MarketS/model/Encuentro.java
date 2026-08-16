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

    // El "default 'PENDIENTE'" en columnDefinition ya cumplió su propósito (permitir agregar
    // esta columna NOT NULL sobre una tabla "encuentros" que ya tenía filas, mismo patrón que
    // User.aceptoTerminos) y se saca: con la columna ya creada, Hibernate lo reinterpretaba en
    // cada arranque como "alter column estado set data type varchar(20) default 'PENDIENTE'",
    // sintaxis inválida en Postgres (el DEFAULT no puede ir pegado a un SET DATA TYPE). El
    // default real lo sigue poniendo el código Java (acá y en EncuentroApiController), la base
    // nunca dependió del default a nivel SQL.
    // PENDIENTE hasta que la contraparte confirma; cualquier edición de lugar/hora/nota vuelve
    // a poner el encuentro en PENDIENTE porque cambiaron los términos acordados.
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private String estado = "PENDIENTE";

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
