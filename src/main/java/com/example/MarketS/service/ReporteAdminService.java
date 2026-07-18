package com.example.MarketS.service;

import com.example.MarketS.dto.ReporteAdminDTO;
import com.example.MarketS.model.Categorias;
import com.example.MarketS.model.Marca;
import com.example.MarketS.model.Producto;
import com.example.MarketS.repository.CategoriaRepository;
import com.example.MarketS.repository.MarcaRepository;
import com.example.MarketS.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.ChartUtils;
import org.jfree.data.general.DefaultPieDataset;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
public class ReporteAdminService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriasRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    public String generarGraficoBarrasBase64(double promedioPrecio, double promedioStock) throws Exception {
        var dataset = new org.jfree.data.category.DefaultCategoryDataset();
        dataset.addValue(promedioPrecio, "Promedio", "Precio");
        dataset.addValue(promedioStock, "Promedio", "Stock");

        JFreeChart chart = ChartFactory.createBarChart(
                "Promedio Precio vs Stock",
                "Tipo",
                "Valor",
                dataset
        );

        chart.setBackgroundPaint(Color.WHITE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, 500, 400);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }


    public ReporteAdminDTO generarReporte() {

        List<Producto> productos = productoRepository.findAll();
        List<Categorias> categorias = categoriasRepository.findAll();
        List<Marca> marcas = marcaRepository.findAll();

        int totalProductos = productos.size();
        int totalCategorias = categorias.size();
        int totalMarcas = (int) productos.stream()
                .map(Producto::getMarca)
                .filter(java.util.Objects::nonNull)
                .map(Marca::getId)
                .distinct()
                .count();

        double promedioPrecio = productos.stream()
                .map(Producto::getPrecio)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double promedioStock = productos.stream()
                .map(Producto::getStock)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        // Nombres de categorías
        List<String> nombresCategorias = categorias.stream()
                .map(Categorias::getNombreCategoria)
                .collect(Collectors.toList());

        // Totales por categoría
        List<Integer> totalesCategorias = categorias.stream()
                .map(c -> productoRepository.countByCategoriasId(c.getId()))
                .collect(Collectors.toList());

        // DTO
        ReporteAdminDTO dto = new ReporteAdminDTO();
        dto.setTotalProductos(totalProductos);
        dto.setTotalCategorias(totalCategorias);
        dto.setTotalMarcas(totalMarcas);
        dto.setPromedioPrecio(promedioPrecio);
        dto.setPromedioStock(promedioStock);
        dto.setNombresCategorias(nombresCategorias);
        dto.setTotalesCategorias(totalesCategorias);

        // Gráficos base64 (por ahora vacío)
        dto.setGraficoCategoriasBase64("");
        dto.setGraficoPreciosStockBase64("");



        try {
            String graficoCategoriasBase64 = generarGraficoTortaBase64(
                    dto.getNombresCategorias(), dto.getTotalesCategorias());
            dto.setGraficoCategoriasBase64(graficoCategoriasBase64);
        } catch (Exception e) {
            dto.setGraficoCategoriasBase64("");
        }

        try {
            // Gráfico de torta
            String graficoCategoriasBase64 = generarGraficoTortaBase64(
                    dto.getNombresCategorias(), dto.getTotalesCategorias());
            dto.setGraficoCategoriasBase64(graficoCategoriasBase64);

            // Gráfico de barras (precio y stock)
            String graficoBarrasBase64 = generarGraficoBarrasBase64(
                    dto.getPromedioPrecio(), dto.getPromedioStock());
            dto.setGraficoPreciosStockBase64(graficoBarrasBase64);

        } catch (Exception e) {
            dto.setGraficoCategoriasBase64("");
            dto.setGraficoPreciosStockBase64("");
        }


        System.out.println("Promedio Precio: " + dto.getPromedioPrecio());
        System.out.println("Promedio Stock: " + dto.getPromedioStock());
        return dto;


    }
    /**
     * Genera un gráfico de torta de categorías en Base64
     */
    public String generarGraficoTortaBase64(List<String> nombres, List<Integer> valores) throws Exception {
        // Imports necesarios: DefaultPieDataset, JFreeChart, PiePlot, ChartUtils, ByteArrayOutputStream, Base64
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (int i = 0; i < nombres.size(); i++) {
            dataset.setValue(nombres.get(i), valores.get(i));
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "Productos por Categoría",
                dataset,
                true,
                true,
                false
        );

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, 500, 400);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}
