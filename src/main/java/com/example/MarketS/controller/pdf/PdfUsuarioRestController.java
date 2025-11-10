package com.example.MarketS.controller.pdf;

import com.example.MarketS.client.pdf.UsuarioPdfClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class PdfUsuarioRestController {

    private final UsuarioPdfClient usuarioPdfClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PdfUsuarioRestController(UsuarioPdfClient usuarioPdfClient) {
        this.usuarioPdfClient = usuarioPdfClient;
    }

    @GetMapping(value = "/api/pdf/usuarios", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generarPdfYJson() {
        try {
            // 1️⃣ Consumir servicio interno
            Map<String, Object> estadisticas = usuarioPdfClient.consumirEstadisticas();

            // 2️⃣ Crear archivo JSON en la raíz
            String jsonFileName = "estadisticas_usuarios_" + LocalDateTime.now().toString().replace(":", "-") + ".json";
            File jsonFile = new File(System.getProperty("user.dir") + "/" + jsonFileName);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, estadisticas);

            // 3️⃣ Generar PDF
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            com.itextpdf.text.Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLUE);
            Paragraph titulo = new Paragraph("Estadísticas de Usuarios", tituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20);
            document.add(titulo);

            // Tabla
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(60);
            table.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell("Categoría");
            table.addCell("Cantidad");

            for (Map.Entry<String, Object> entry : estadisticas.entrySet()) {
                table.addCell(entry.getKey());
                table.addCell(entry.getValue().toString());
            }
            document.add(table);
            document.add(new Paragraph(" "));

            // 4️⃣ Gráfico de barras
            DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
            for (Map.Entry<String, Object> entry : estadisticas.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    barDataset.addValue(((Number) entry.getValue()).doubleValue(), "Usuarios", entry.getKey());
                }
            }

            var barChart = ChartFactory.createBarChart(
                    "Distribución de Usuarios por Rol",
                    "Rol",
                    "Cantidad",
                    barDataset
            );

            File barChartFile = File.createTempFile("grafico_barras", ".png");
            ChartUtils.saveChartAsPNG(barChartFile, barChart, 500, 300);

            Image barImage = Image.getInstance(barChartFile.getAbsolutePath());
            barImage.setAlignment(Element.ALIGN_CENTER);
            document.add(barImage);
            document.add(new Paragraph(" "));

            // 5️⃣ Gráfico de torta
            DefaultPieDataset pieDataset = new DefaultPieDataset();
            for (Map.Entry<String, Object> entry : estadisticas.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    pieDataset.setValue(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                }
            }

            var pieChart = ChartFactory.createPieChart(
                    "Porcentaje de Usuarios por Rol",
                    pieDataset,
                    true,
                    true,
                    false
            );

            File pieChartFile = File.createTempFile("grafico_torta", ".png");
            ChartUtils.saveChartAsPNG(pieChartFile, pieChart, 500, 300);

            Image pieImage = Image.getInstance(pieChartFile.getAbsolutePath());
            pieImage.setAlignment(Element.ALIGN_CENTER);
            document.add(pieImage);

            document.close();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"estadisticas_usuarios.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(baos.toByteArray());

        } catch (IOException | DocumentException e) {
            e.printStackTrace();
            throw new RuntimeException("Error al generar PDF o gráficos: " + e.getMessage(), e);
        }
    }
}
