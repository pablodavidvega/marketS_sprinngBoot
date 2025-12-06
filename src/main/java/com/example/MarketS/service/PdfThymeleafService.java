package com.example.MarketS.service;

import org.jfree.chart.JFreeChart;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.MarketS.model.Producto;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;
import com.example.MarketS.service.pdf.B64ImgReplacedElementFactory;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

import org.jfree.chart.ChartFactory;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

@Service
public class PdfThymeleafService {

    @Autowired
    private TemplateEngine templateEngine;

    // Conversión seguro gráfico -> Base64
    private String chartToBase64(JFreeChart chart, int width, int height) {
        try {
            BufferedImage image = chart.createBufferedImage(width, height);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // Generar PDF
    public byte[] generarPdfDesdeHtml(List<Producto> productos) {

        // Barras por categoría
        Map<String, Long> productosPorCategoria = productos.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCategorias() != null ?
                                p.getCategorias().getNombreCategoria() : "Sin categoría",
                        Collectors.counting()
                ));

        DefaultCategoryDataset datasetBarras = new DefaultCategoryDataset();
        productosPorCategoria.forEach((cat, count) ->
                datasetBarras.addValue(count, "Productos", cat)
        );

        JFreeChart graficoBarras = ChartFactory.createBarChart(
                "Productos por Categoría", "Categoría", "Cantidad", datasetBarras
        );

        String barrasBase64 = chartToBase64(graficoBarras, 600, 400);


        // Torta activos / inactivos
        long activos = productos.stream().filter(Producto::getActivo).count();
        long inactivos = productos.size() - activos;

        DefaultPieDataset<String> datasetTorta = new DefaultPieDataset<>();
        datasetTorta.setValue("Activos", activos);
        datasetTorta.setValue("Inactivos", inactivos);

        JFreeChart graficoTorta = ChartFactory.createPieChart(
                "Productos Activos vs Inactivos", datasetTorta, true, true, false
        );

        String tortaBase64 = chartToBase64(graficoTorta, 500, 350);

        // Enviar al HTML
        Context context = new Context();
        context.setVariable("productos", productos);
        context.setVariable("graficoBarras", barrasBase64);
        context.setVariable("graficoTorta", tortaBase64);

        String html = templateEngine.process("reporte", context);
        System.out.println("HTML generado:\n" + html);

        // Render PDF
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            ITextRenderer renderer = new ITextRenderer();
            renderer.getSharedContext().setReplacedElementFactory(
                    new B64ImgReplacedElementFactory()
            );

            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);

            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("HTML enviado:");
            System.out.println(html);

            throw new RuntimeException("Error al generar PDF: " + e.getMessage(), e);
        }
    }

}
