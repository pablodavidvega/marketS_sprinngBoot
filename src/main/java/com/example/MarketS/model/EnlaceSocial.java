package com.example.MarketS.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

// Enlace libre (red social, sitio web, lo que sea) que el vendedor agrega desde "Editar
// perfil" para mostrar en su perfil público — ver VendedorPublicController. Lista libre, no un
// set fijo de redes: la etiqueta es texto a elección del vendedor.
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "enlace_social")
public class EnlaceSocial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String etiqueta;

    @Column(nullable = false)
    private String url;

    @ManyToOne
    @JoinColumn(name = "vendedor_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Vendedor vendedor;
}
