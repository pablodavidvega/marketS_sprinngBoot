package com.example.MarketS.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "preguntas")
@Getter
@Setter
@NoArgsConstructor
public class Pregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(columnDefinition = "TEXT")
    private String respuesta;

    @Column(name = "fecha_pregunta", nullable = false)
    private LocalDateTime fechaPregunta;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;
}
