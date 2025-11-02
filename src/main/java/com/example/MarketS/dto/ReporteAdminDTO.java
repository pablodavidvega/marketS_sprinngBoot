package com.example.MarketS.dto;

import java.util.List;

public class ReporteAdminDTO {

    private int totalProductos;
    private int totalCategorias;
    private int totalMarcas;
    private double promedioPrecio;
    private double promedioStock;

    // Listas para gráficos
    private List<String> nombresCategorias;
    private List<Integer> totalesCategorias;

    // Base64 de los gráficos para PDF (opcional)
    private String graficoCategoriasBase64;
    private String graficoPreciosStockBase64;

    // Getters y Setters
    public int getTotalProductos() { return totalProductos; }
    public void setTotalProductos(int totalProductos) { this.totalProductos = totalProductos; }

    public int getTotalCategorias() { return totalCategorias; }
    public void setTotalCategorias(int totalCategorias) { this.totalCategorias = totalCategorias; }

    public int getTotalMarcas() { return totalMarcas; }
    public void setTotalMarcas(int totalMarcas) { this.totalMarcas = totalMarcas; }

    public double getPromedioPrecio() { return promedioPrecio; }
    public void setPromedioPrecio(double promedioPrecio) { this.promedioPrecio = promedioPrecio; }

    public double getPromedioStock() { return promedioStock; }
    public void setPromedioStock(double promedioStock) { this.promedioStock = promedioStock; }

    public List<String> getNombresCategorias() { return nombresCategorias; }
    public void setNombresCategorias(List<String> nombresCategorias) { this.nombresCategorias = nombresCategorias; }

    public List<Integer> getTotalesCategorias() { return totalesCategorias; }
    public void setTotalesCategorias(List<Integer> totalesCategorias) { this.totalesCategorias = totalesCategorias; }

    public String getGraficoCategoriasBase64() { return graficoCategoriasBase64; }
    public void setGraficoCategoriasBase64(String graficoCategoriasBase64) { this.graficoCategoriasBase64 = graficoCategoriasBase64; }

    public String getGraficoPreciosStockBase64() { return graficoPreciosStockBase64; }
    public void setGraficoPreciosStockBase64(String graficoPreciosStockBase64) { this.graficoPreciosStockBase64 = graficoPreciosStockBase64; }
}
