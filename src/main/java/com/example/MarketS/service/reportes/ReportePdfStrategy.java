package com.example.MarketS.service.reportes;

import com.example.MarketS.dto.ReporteAdminDTO;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;

public class ReportePdfStrategy implements ReporteStrategy {

    @Override
    public void exportar(HttpServletResponse response, ReporteAdminDTO reporte) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=reporte.pdf");

        try {
            Document documento = new Document();
            PdfWriter.getInstance(documento, response.getOutputStream());
            documento.open();

            // Título
            Font tituloFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            documento.add(new Paragraph("📊 Reporte de Productos", tituloFont));
            documento.add(new Paragraph("Generado con el patrón Strategy\n\n"));

            // Datos principales
            documento.add(new Paragraph("Total de Productos: " + reporte.getTotalProductos()));
            documento.add(new Paragraph("Total de Categorías: " + reporte.getTotalCategorias()));
            documento.add(new Paragraph("Total de Marcas: " + reporte.getTotalMarcas()));
            documento.add(new Paragraph("Promedio de Precio: " + reporte.getPromedioPrecio()));
            documento.add(new Paragraph("Promedio de Stock: " + reporte.getPromedioStock()));
            documento.add(new Paragraph("\n"));

            // Agregar gráfico si existe
            if (reporte.getGraficoCategoriasBase64() != null && !reporte.getGraficoCategoriasBase64().isEmpty()) {
                byte[] imageBytes = Base64.getDecoder().decode(reporte.getGraficoCategoriasBase64());
                Image grafico = Image.getInstance(imageBytes);
                grafico.scaleToFit(500, 400);
                grafico.setAlignment(Image.ALIGN_CENTER);
                documento.add(grafico);
            }
            // Segundo gráfico: barras (precio vs stock)
            if (reporte.getGraficoPreciosStockBase64() != null && !reporte.getGraficoPreciosStockBase64().isEmpty()) {
                documento.add(new Paragraph("\n"));
                byte[] barrasBytes = Base64.getDecoder().decode(reporte.getGraficoPreciosStockBase64());
                Image graficoBarras = Image.getInstance(barrasBytes);
                graficoBarras.scaleToFit(500, 400);
                graficoBarras.setAlignment(Image.ALIGN_CENTER);
                documento.add(graficoBarras);
            } else {
                documento.add(new Paragraph("(Sin gráfico disponible)"));
            }

            documento.close();

        } catch (Exception e) {
            throw new IOException("Error al generar PDF", e);
        }
    }
}
