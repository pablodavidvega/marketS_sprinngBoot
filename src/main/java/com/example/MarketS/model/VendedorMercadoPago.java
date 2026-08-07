package com.example.MarketS.model;

import com.example.MarketS.security.TokenCryptoConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Cuenta de MercadoPago que el vendedor conectó vía OAuth (ver MercadoPagoOAuthService) para
// que el dinero de sus ventas vaya directo a su cuenta, en vez de a la de la plataforma. Misma
// fila también guarda el "state" transitorio del flujo de conexión en curso (se limpia al
// completarse) para no necesitar una tabla aparte para eso.
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vendedor_mercado_pago")
public class VendedorMercadoPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @ToString.Exclude
    @JsonIgnore
    private User user;

    private String mpUserId;
    private String mpEmail;

    @Convert(converter = TokenCryptoConverter.class)
    @Column(length = 1000)
    @JsonIgnore
    private String accessToken;

    @Convert(converter = TokenCryptoConverter.class)
    @Column(length = 1000)
    @JsonIgnore
    private String refreshToken;

    private LocalDateTime expiraEn;
    private LocalDateTime conectadoEn;

    // Estado transitorio de una conexión en progreso (entre "iniciarConexion" y el callback de
    // MP); null cuando no hay una conexión en curso.
    private String state;
    private LocalDateTime stateCreadoEn;

    public boolean estaConectado() {
        return accessToken != null;
    }
}
