package com.example.MarketS.service.reportes;

import com.example.MarketS.dto.ReporteAdminDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.IOException;

public class ReporteExcelStrategy implements ReporteStrategy {

    @Override
    public void exportar(HttpServletResponse response, ReporteAdminDTO reporte) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=reporte.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte Admin");

            // Encabezados
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Total Productos", "Total Categorías", "Total Marcas", "Promedio Precio", "Promedio Stock"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Datos
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(reporte.getTotalProductos());
            dataRow.createCell(1).setCellValue(reporte.getTotalCategorias());
            dataRow.createCell(2).setCellValue(reporte.getTotalMarcas());
            dataRow.createCell(3).setCellValue(reporte.getPromedioPrecio());
            dataRow.createCell(4).setCellValue(reporte.getPromedioStock());

            // Autoajustar columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        } catch (Exception e) {
            throw new IOException("Error al generar Excel", e);
        }
    }
}
