package com.example.MarketS.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "banner_slide")
@Getter
@Setter
public class BannerSlide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imagen;

    // Opcional: versión para celular (< 640 px). Si es null se usa "imagen" en todas las pantallas.
    @Column(name = "imagen_mobile")
    private String imagenMobile;

    @Column(nullable = false)
    private String texto;

    @Column(nullable = false)
    private String enlace;

    @Column(nullable = false)
    private int orden = 0;

    @Column(nullable = false)
    private boolean activo = true;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_creacion")
    private Date fechaCreacion = new Date();
}
