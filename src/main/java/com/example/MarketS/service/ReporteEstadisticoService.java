package com.example.MarketS.service;

import com.example.MarketS.model.Rol;
import com.example.MarketS.repository.RolRepository;
import com.example.MarketS.repository.UserRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

@Service
public class ReporteEstadisticoService {

    private final RolRepository rolRepository;
    private final UserRepository userRepository;

    public ReporteEstadisticoService(RolRepository rolRepository, UserRepository userRepository) {
        this.rolRepository = rolRepository;
        this.userRepository = userRepository;
    }


        public byte[] generarReporteEstadistico() throws com.itextpdf.text.DocumentException, java.io.IOException {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // ✅ usar la clase Font de iText con el nombre completo
        com.itextpdf.text.Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph titulo = new Paragraph("📊 Reporte Estadístico de Usuarios", fontTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);
        document.add(Chunk.NEWLINE);

        // Crear dataset para gráfico
        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        List<Rol> roles = rolRepository.findAll();
        for (Rol rol : roles) {
            long cantidad = userRepository.countByRol(rol);
            dataset.setValue(rol.getRolname(), cantidad);
        }

        // Crear gráfico de torta
        JFreeChart chart = ChartFactory.createPieChart(
                "Usuarios por Rol",
                dataset,
                true, true, false);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionPaint("ROLE_ADMIN", Color.RED);
        plot.setSectionPaint("ROLE_CLIENTE", Color.GREEN);
        plot.setSectionPaint("ROLE_VENDEDOR", Color.BLUE);
        plot.setLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12));

        BufferedImage chartImage = chart.createBufferedImage(500, 300);
        ByteArrayOutputStream chartOut = new ByteArrayOutputStream();
        ImageIO.write(chartImage, "png", chartOut);

        com.itextpdf.text.Image image = com.itextpdf.text.Image.getInstance(chartOut.toByteArray());
        image.setAlignment(Element.ALIGN_CENTER);
        document.add(image);
        document.add(Chunk.NEWLINE);

        // Tabla de resumen
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.addCell("Rol");
        table.addCell("Cantidad de Usuarios");

        for (Rol rol : roles) {
            long cantidad = userRepository.countByRol(rol);
            table.addCell(rol.getRolname());
            table.addCell(String.valueOf(cantidad));
        }

        document.add(table);
        document.close();

        return out.toByteArray();
    }
}
