package com.example.MarketS.service;

import com.example.MarketS.model.User;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class ReportePdfService {

    public byte[] generarReporteUsuarios(List<User> usuarios) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());

        PdfWriter.getInstance(document, baos);
        document.open();

        // Título
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("Reporte de Usuarios", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph(" "));

        // Tabla
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new int[]{1, 3, 4, 2});

        // Encabezados
        Font headFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        table.addCell(new PdfPCell(new Phrase("ID", headFont)));
        table.addCell(new PdfPCell(new Phrase("Nombre", headFont)));
        table.addCell(new PdfPCell(new Phrase("Email", headFont)));
        table.addCell(new PdfPCell(new Phrase("Rol", headFont)));

        // Datos
        for (User user : usuarios) {
            table.addCell(String.valueOf(user.getId()));
            table.addCell(user.getUsername());
            table.addCell(user.getEmail());
            table.addCell(user.getRol() != null ? user.getRol().getRolname() : "");
        }

        document.add(table);
        document.close();

        return baos.toByteArray();
    }
}
