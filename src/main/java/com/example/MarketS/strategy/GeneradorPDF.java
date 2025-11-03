package com.example.MarketS.strategy;

import com.example.MarketS.dto.ReporteAdminDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.IOException;
import java.util.Base64;

@Component
public class GeneradorPDF implements GeneradorReporteStrategy {

    @Override
    public void generar(ReporteAdminDTO dto, HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_admin.pdf");

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            // Título y datos
            document.add(new Paragraph("REPORTE PRODUCTOS"));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total Productos: " + dto.getTotalProductos()));
            document.add(new Paragraph("Total Categorías: " + dto.getTotalCategorias()));
            document.add(new Paragraph("Total Marcas: " + dto.getTotalMarcas()));
            document.add(new Paragraph("Promedio Precio: " + dto.getPromedioPrecio()));
            document.add(new Paragraph("Promedio Stock: " + dto.getPromedioStock()));
            document.add(new Paragraph(" "));

            // Gráfico
            String base64 = dto.getGraficoCategoriasBase64();
            if (base64 != null && !base64.isEmpty()) {
                byte[] imageBytes = Base64.getDecoder().decode(base64);
                Image chartImage = Image.getInstance(imageBytes);
                chartImage.scaleToFit(500, 400);
                chartImage.setAlignment(Element.ALIGN_CENTER);
                document.add(chartImage);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Error generando PDF", e);
        } finally {
            document.close();
        }
    }
}
