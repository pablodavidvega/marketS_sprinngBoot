package com.example.MarketS.dto;

public class TallasStockDTO {
    private String talla;
    private Integer stock;  // Cambia de "int" a "Integer"

    public TallasStockDTO() {
    }

    public TallasStockDTO(String talla, Integer stock) {  // Cambia a Integer
        this.talla = talla;
        this.stock = stock;
    }

    public String getTalla() {
        return talla;
    }

    public void setTalla(String talla) {
        this.talla = talla;
    }

    public Integer getStock() {  // Cambia a Integer
        return stock;
    }

    public void setStock(Integer stock) {  // Cambia a Integer
        this.stock = stock;
    }
}