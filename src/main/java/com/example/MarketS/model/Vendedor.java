package com.example.MarketS.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

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
}
