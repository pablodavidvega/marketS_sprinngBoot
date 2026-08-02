package com.example.MarketS.service.reportes;

import com.example.MarketS.dto.ReporteAdminDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.IOException;
import java.util.List;

public class ReporteExcelStrategy implements ReporteStrategy {

    @Override
    public void exportar(HttpServletResponse response, ReporteAdminDTO reporte) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=reporte.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte Admin");

            // Encabezados del resumen
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Total Productos", "Total Categorías", "Total Marcas", "Promedio Precio", "Promedio Stock"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Datos del resumen
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue(reporte.getTotalProductos());
            dataRow.createCell(1).setCellValue(reporte.getTotalCategorias());
            dataRow.createCell(2).setCellValue(reporte.getTotalMarcas());
            dataRow.createCell(3).setCellValue(reporte.getPromedioPrecio());
            dataRow.createCell(4).setCellValue(reporte.getPromedioStock());

            // Desglose de productos por categoría (mismos datos que el CSV/PDF/gráficas)
            int filaActual = 3;
            Row tituloDesglose = sheet.createRow(filaActual++);
            tituloDesglose.createCell(0).setCellValue("Productos por categoría");

            Row headerDesglose = sheet.createRow(filaActual++);
            headerDesglose.createCell(0).setCellValue("Categoría");
            headerDesglose.createCell(1).setCellValue("Total Productos");

            List<String> nombresCategorias = reporte.getNombresCategorias();
            List<Integer> totalesCategorias = reporte.getTotalesCategorias();
            if (nombresCategorias != null) {
                for (int i = 0; i < nombresCategorias.size(); i++) {
                    Row fila = sheet.createRow(filaActual++);
                    fila.createCell(0).setCellValue(nombresCategorias.get(i));
                    int total = (totalesCategorias != null && i < totalesCategorias.size())
                            ? totalesCategorias.get(i) : 0;
                    fila.createCell(1).setCellValue(total);
                }
            }

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
