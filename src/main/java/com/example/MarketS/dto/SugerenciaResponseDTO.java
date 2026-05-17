package com.example.MarketS.dto;

public record SugerenciaResponseDTO(
        Long id,
        String nombre,
        double precio,
        String imagen,
        String categoriaNombre
) {}
