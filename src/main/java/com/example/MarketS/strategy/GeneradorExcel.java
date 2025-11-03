package com.example.MarketS.strategy;

import com.example.MarketS.dto.ReporteAdminDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class GeneradorExcel implements GeneradorReporteStrategy {

    @Override
    public void generar(ReporteAdminDTO dto, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_admin.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte Productos");
            int rowIdx = 0;

            // Fila encabezado
            Row header = sheet.createRow(rowIdx++);
            header.createCell(0).setCellValue("Total Productos");
            header.createCell(1).setCellValue("Total Categorías");
            header.createCell(2).setCellValue("Total Marcas");
            header.createCell(3).setCellValue("Promedio Precio");
            header.createCell(4).setCellValue("Promedio Stock");

            // Fila de datos
            Row data = sheet.createRow(rowIdx);
            data.createCell(0).setCellValue(dto.getTotalProductos());
            data.createCell(1).setCellValue(dto.getTotalCategorias());
            data.createCell(2).setCellValue(dto.getTotalMarcas());
            data.createCell(3).setCellValue(dto.getPromedioPrecio());
            data.createCell(4).setCellValue(dto.getPromedioStock());

            workbook.write(response.getOutputStream());
        }
    }
}
