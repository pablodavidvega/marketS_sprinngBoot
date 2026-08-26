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

    // true si el usuario conoce su contraseña actual (la puso él mismo al registrarse o al
    // restablecerla). false para altas por Google (ver GoogleAuthApiController.completar), que
    // reciben una password aleatoria que nunca ven — así el perfil puede ofrecer "crear
    // contraseña" en vez de "cambiar contraseña" sin pedir una que no conocen.
    // Default true a nivel de columna: mismo patrón que "acepto_terminos" para que Hibernate
    // pueda agregar esta columna NOT NULL sobre una tabla "users" con filas existentes.
    @Column(name = "password_establecida", nullable = false, columnDefinition = "boolean default true")
    private boolean passwordEstablecida = true;

    // true para altas por carga masiva (ver CargaMasivaUsuariosService.construirUsuario): la
    // contraseña la elige el admin en el CSV/Excel, no el usuario, y pudo circular por email o
    // en papel — se obliga a cambiarla en el primer login (ver AuthApiController.login +
    // UsuarioApiController.cambiarPassword, que la apaga al completar el cambio).
    // Default false a nivel de columna: mismo patrón que "acepto_terminos"/"password_establecida".
    @Column(name = "debe_cambiar_password", nullable = false, columnDefinition = "boolean default false")
    private boolean debeCambiarPassword = false;

    @ManyToOne
    @JoinColumn(name="rol_id")
    private Rol rol;

    //No es dueña de la relacion, por eso es el mapeo(Join)//
    @OneToOne(mappedBy = "user",cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Vendedor vendedor;

}
