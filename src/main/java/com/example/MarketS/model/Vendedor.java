package com.example.MarketS.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = "vendedores")
public class Vendedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
@Column(unique = false, nullable = false)
    private String city;

//Relacion One to One//
@OneToOne
    @JoinColumn(name = "user_id", nullable = false)
@ToString.Exclude
    @JsonIgnore
    private User user;

    // Enlaces libres (redes sociales, sitio web, etc.) que el vendedor administra desde
    // "Editar perfil" — se guardan con el patrón "limpiar y reconstruir" (ver
    // UsuarioApiController.actualizarEnlaces), por eso orphanRemoval=true: sin esto, un
    // enlaces.clear() en Java no borraría las filas viejas en la base.
    @OneToMany(mappedBy = "vendedor", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<EnlaceSocial> enlaces = new ArrayList<>();
}
