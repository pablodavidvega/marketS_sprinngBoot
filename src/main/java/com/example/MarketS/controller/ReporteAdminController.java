package com.example.MarketS.controller;

import com.example.MarketS.dto.ReporteAdminDTO;
import com.example.MarketS.service.ReporteAdminService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.Base64;

@Controller
public class ReporteAdminController {

    @Autowired
    private ReporteAdminService reporteService;

    @GetMapping("/admin/reporte-pdf")
    public String mostrarEstadisticas(org.springframework.ui.Model model) {
        ReporteAdminDTO reporte = reporteService.generarReporteGlobal();
        model.addAttribute("reporte", reporte);
        return "reporte-pdf-admin/estadisticas-admin";
    }

    @GetMapping("/admin/reporte-pdf/descargar")
    public void descargarPdf(HttpServletResponse response) throws IOException {
        ReporteAdminDTO dto = reporteService.generarReporteGlobal();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_admin.pdf");

        com.itextpdf.text.Document document = new com.itextpdf.text.Document();
        try {
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, response.getOutputStream());
            document.open();

            // Título y datos
            document.add(new com.itextpdf.text.Paragraph("REPORTE PRODUCTOS"));
            document.add(new com.itextpdf.text.Paragraph(" "));
            document.add(new com.itextpdf.text.Paragraph("Total Productos: " + dto.getTotalProductos()));
            document.add(new com.itextpdf.text.Paragraph("Total Categorías: " + dto.getTotalCategorias()));
            document.add(new com.itextpdf.text.Paragraph("Total Marcas: " + dto.getTotalMarcas()));
            document.add(new com.itextpdf.text.Paragraph("Promedio Precio: " + dto.getPromedioPrecio()));
            document.add(new com.itextpdf.text.Paragraph("Promedio Stock: " + dto.getPromedioStock()));
            document.add(new com.itextpdf.text.Paragraph(" "));

            // Generar gráfico y agregarlo
            String base64 = reporteService.generarGraficoTortaBase64(dto.getNombresCategorias(), dto.getTotalesCategorias());
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            com.itextpdf.text.Image chartImage = com.itextpdf.text.Image.getInstance(imageBytes);
            chartImage.scaleToFit(500, 400);
            chartImage.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            document.add(chartImage);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Error generando el PDF con gráfico", e);
        } finally {
            document.close(); // cerrar manualmente
        }
    }

}
