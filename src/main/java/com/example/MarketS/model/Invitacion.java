package com.example.MarketS.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "invitacion")
@Getter
@Setter
@NoArgsConstructor
public class Invitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String token;

    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "usos_actuales", nullable = false)
    private int usosActuales = 0;

    @Column(name = "max_usos")
    private Integer maxUsos;

    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creada_por")
    private User creadaPor;

    public boolean isVigente() {
        if (!activo) return false;
        if (maxUsos != null && usosActuales >= maxUsos) return false;
        if (fechaExpiracion != null && LocalDateTime.now().isAfter(fechaExpiracion)) return false;
        return true;
    }
}
