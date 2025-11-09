package com.example.MarketS.controller.pdf;

import com.example.MarketS.client.pdf.UsuarioPdfClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.File;
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
            // 🔹 1. Consumir servicio interno
            Map<String, Object> estadisticas = usuarioPdfClient.consumirEstadisticas();

            // 🔹 2. Crear archivo JSON en la raíz
            String jsonFileName = "estadisticas_usuarios_" + LocalDateTime.now().toString().replace(":", "-") + ".json";
            File jsonFile = new File(System.getProperty("user.dir") + "/" + jsonFileName);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, estadisticas);

            // 🔹 3. Generar PDF
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLUE);
            Paragraph titulo = new Paragraph("Estadísticas de Usuarios", tituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20);
            document.add(titulo);

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
            document.close();

            // 🔹 4. Retornar PDF a Postman
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"estadisticas_usuarios.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(baos.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al generar PDF o JSON: " + e.getMessage(), e);
        }
    }
}
