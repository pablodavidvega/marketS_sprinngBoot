package com.example.MarketS.dto;

import com.example.MarketS.model.Categorias;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoriaAdminDTO {

    private Long id;
    private String nombreCategoria;
    private long totalProductos;

    public static CategoriaAdminDTO from(Categorias c, long totalProductos) {
        return CategoriaAdminDTO.builder()
                .id(c.getId())
                .nombreCategoria(c.getNombreCategoria())
                .totalProductos(totalProductos)
                .build();
    }
}
