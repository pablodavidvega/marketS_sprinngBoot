package com.example.MarketS.carga.dto;

import lombok.Data;

@Data
public class ProductoArchivoDTO {

    private String nombre;
    private Double precio;
    private Integer stock;
    private String descripcion;
    private String imagen;     // Base64 o ruta
    private String marca;
    private String categoria;
}
