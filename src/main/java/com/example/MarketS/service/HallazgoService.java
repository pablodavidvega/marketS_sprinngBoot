package com.example.MarketS.service;

import com.example.MarketS.model.Hallazgo;
import com.example.MarketS.repository.HallazgoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.OutputStream;


@Service
public class HallazgoService {

    @Autowired
    private HallazgoRepository repository;

    public void registrarHallazgo(String prueba, String tipo, boolean exito, String descripcion) {
        Hallazgo h = new Hallazgo();
        h.setPrueba(prueba);
        h.setTipo(tipo);
        h.setResultado(exito ? "Aprobada" : "Fallida");
        h.setDescripcion(descripcion);
        h.setFecha(LocalDateTime.now());
        repository.save(h);
    }

    public List<Hallazgo> obtenerTodos() {
        return repository.findAll();
    }

    // Método para exportar PDF
    public void exportarPdf(OutputStream outputStream) {
        List<Hallazgo> hallazgos = repository.findAll();

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            document.add(new Paragraph("Reporte de Hallazgos de Pruebas\n\n"));

            PdfPTable table = new PdfPTable(5); // 5 columnas
            table.addCell("ID");
            table.addCell("Prueba");
            table.addCell("Tipo");
            table.addCell("Resultado");
            table.addCell("Descripción");

            for (Hallazgo h : hallazgos) {
                table.addCell(String.valueOf(h.getId()));
                table.addCell(h.getPrueba());
                table.addCell(h.getTipo());
                table.addCell(h.getResultado());
                table.addCell(h.getDescripcion());
            }

            document.add(table);
        } catch (DocumentException e) {
            e.printStackTrace();
        } finally {
            document.close();
        }
    }
}
