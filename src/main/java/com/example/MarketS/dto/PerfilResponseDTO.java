package com.example.MarketS.dto;

import com.example.MarketS.model.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PerfilResponseDTO {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String address;
    private String rol;
    private String city;
    private LocalDateTime createdAt;

    // Un solo usuario: aquí sí es seguro tocar el fetch LAZY de Vendedor (una única consulta
    // extra, no hay N+1 posible con un solo User).
    public static PerfilResponseDTO from(User user) {
        boolean esVendedor = "ROLE_VENDEDOR".equals(user.getRol().getRolname());
        return from(user, esVendedor && user.getVendedor() != null ? user.getVendedor().getCity() : null);
    }

    // Variante para listados: recibe la ciudad ya resuelta en batch (ver
    // VendedorRepository.findCityByUserIdIn) en vez de tocar user.getVendedor(), que dispara
    // una consulta extra por cada vendedor de la lista (OneToOne mappedBy sin bytecode
    // enhancement — ver comentario en User.vendedor).
    public static PerfilResponseDTO from(User user, String city) {
        return PerfilResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .rol(user.getRol().getRolname())
                .city(city)
                .createdAt(user.getCreatedAt())
                .build();
    }

    // Variante para listados grandes: arma el DTO directo desde una fila escalar (ver
    // UserRepository.findAllPerfilProjection) sin pasar nunca por la entidad User — evita el
    // disparo automático de Hibernate al inicializar User.vendedor (ver comentario arriba).
    public static PerfilResponseDTO fromProjection(Object[] row, String city) {
        return PerfilResponseDTO.builder()
                .id(((Number) row[0]).longValue())
                .username((String) row[1])
                .email((String) row[2])
                .phone((String) row[3])
                .address((String) row[4])
                .rol((String) row[5])
                .city(city)
                .createdAt((LocalDateTime) row[6])
                .build();
    }
}
