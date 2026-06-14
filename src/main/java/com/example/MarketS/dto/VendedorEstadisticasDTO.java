package com.example.MarketS.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VendedorEstadisticasDTO {
    private Long id;
    private String username;
    private String email;
    private Long totalProductos;
    private Long totalUnidades;
    private Double gananciaTotal;
    private Double comisionTotal;
    private Double ingresosTotal;

    public static VendedorEstadisticasDTO from(Object[] row) {
        return VendedorEstadisticasDTO.builder()
                .id(((Number) row[0]).longValue())
                .username((String) row[1])
                .email((String) row[2])
                .totalProductos(((Number) row[3]).longValue())
                .totalUnidades(((Number) row[4]).longValue())
                .gananciaTotal(((Number) row[5]).doubleValue())
                .comisionTotal(((Number) row[6]).doubleValue())
                .ingresosTotal(((Number) row[7]).doubleValue())
                .build();
    }
}
