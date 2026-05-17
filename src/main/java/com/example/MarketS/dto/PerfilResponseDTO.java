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
        return PerfilResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .rol(user.getRol().getRolname())
                .city(user.getVendedor() != null ? user.getVendedor().getCity() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
