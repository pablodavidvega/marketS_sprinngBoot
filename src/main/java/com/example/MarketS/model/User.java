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
    @Column(unique = true, nullable = false)
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

    // Identificador único de Google (claim "sub" del id_token) para usuarios que se
    // loguean/registran con Google. Nullable: los usuarios sin Google vinculado quedan en
    // null, y Postgres permite múltiples NULL en una columna UNIQUE sin problema.
    @Column(name = "google_id", unique = true)
    private String googleId;

    // columnDefinition con DEFAULT: permite que Hibernate agregue esta columna NOT NULL
    // sobre una tabla "users" que ya tiene filas (Postgres backfillea con el default en
    // vez de rechazar el ALTER TABLE por valores nulos existentes).
    @Column(name = "acepto_terminos", nullable = false, columnDefinition = "boolean default false")
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
