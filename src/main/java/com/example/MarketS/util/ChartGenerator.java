package com.example.MarketS.util;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtils;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class ChartGenerator {

    // Generar gráfico de torta
    public static byte[] generarGraficoTorta(Map<String, Object> datos) throws IOException {
        DefaultPieDataset dataset = new DefaultPieDataset<>();
        dataset.setValue("Clientes", (Number) datos.get("Clientes"));
        dataset.setValue("Admins", (Number) datos.get("Admins"));
        dataset.setValue("Vendedores", (Number) datos.get("Vendedores"));

        JFreeChart chart = ChartFactory.createPieChart(
                "Distribución de Usuarios por Rol", dataset, true, true, false);
        chart.setBackgroundPaint(Color.WHITE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, 500, 350);
        return baos.toByteArray();
    }

    // Generar gráfico de barras
    public static byte[] generarGraficoBarras(Map<String, Object> datos) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue((Number) datos.get("Clientes"), "Usuarios", "Clientes");
        dataset.addValue((Number) datos.get("Admins"), "Usuarios", "Admins");
        dataset.addValue((Number) datos.get("Vendedores"), "Usuarios", "Vendedores");

        JFreeChart chart = ChartFactory.createBarChart(
                "Usuarios por Rol", "Rol", "Cantidad", dataset);
        chart.setBackgroundPaint(Color.WHITE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(baos, chart, 500, 350);
        return baos.toByteArray();
    }
}
