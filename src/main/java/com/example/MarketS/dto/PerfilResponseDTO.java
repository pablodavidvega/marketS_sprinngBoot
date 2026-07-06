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

    public static PerfilResponseDTO from(User user) {
        // Evita disparar el fetch LAZY de Vendedor (una consulta extra a la BD) para
        // usuarios que no son vendedores, ya que nunca tendrán fila asociada.
        boolean esVendedor = "ROLE_VENDEDOR".equals(user.getRol().getRolname());

        return PerfilResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .rol(user.getRol().getRolname())
                .city(esVendedor && user.getVendedor() != null ? user.getVendedor().getCity() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
