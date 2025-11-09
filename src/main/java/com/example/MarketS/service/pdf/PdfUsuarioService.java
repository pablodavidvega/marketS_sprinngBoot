package com.example.MarketS.service.pdf;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.util.Map;

@Service
public class PdfUsuarioService {

    public void generarPdf(Map<String, Object> datos, String rutaArchivo) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(rutaArchivo));
            document.open();

            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph titulo = new Paragraph("Estadísticas de Usuarios", tituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(new Paragraph(" "));

            for (Map.Entry<String, Object> entry : datos.entrySet()) {
                document.add(new Paragraph(entry.getKey() + ": " + entry.getValue()));
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
