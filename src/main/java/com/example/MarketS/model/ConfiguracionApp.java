package com.example.MarketS.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "configuracion_app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionApp {

    @Id
    private String clave;

    @Column(length = 500)
    private String valor;
}
