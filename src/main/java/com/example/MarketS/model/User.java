package com.example.MarketS.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users", indexes = @Index(name = "idx_users_email", columnList = "email"))
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    private String email;
    private String password;
    @Column(unique = false, nullable = false)
    private String phone;
    private String address;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private String resetToken;
    private LocalDateTime resetTokenExpira;

    @Column(name = "acepto_terminos", nullable = false)
    private boolean aceptoTerminos;

    @Column(name = "fecha_aceptacion_terminos")
    private LocalDateTime fechaAceptacionTerminos;

    @ManyToOne
    @JoinColumn(name="rol_id")
    private Rol rol;

    //No es dueña de la relacion, por eso es el mapeo(Join)//
    @OneToOne(mappedBy = "user",cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Vendedor vendedor;

}
