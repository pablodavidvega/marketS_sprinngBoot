package com.example.MarketS.carga.strategy;

import com.example.MarketS.carga.dto.DTOResultadoCarga;
import com.example.MarketS.model.Producto;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Component("excelStrategy")
public class ExcelProductoCargaStrategy implements CargaArchivoStrategy {

    @Override
    public DTOResultadoCarga procesar(MultipartFile file) {

        List<Producto> productos = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        try {
            Workbook workbook = WorkbookFactory.create(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);

            int fila = 0;

            for (Row row : sheet) {

                if (fila == 0) { fila++; continue; }

                try {
                    Producto p = new Producto();

                    p.setNombre(getValue(row, 0));
                    p.setPrecio(Double.parseDouble(getValue(row, 1)));
                    p.setDescripcion(getValue(row, 2));
                    p.setImagen(getValue(row, 3));
                    p.setStock(Integer.parseInt(getValue(row, 4)));

                    productos.add(p);

                } catch (Exception e) {
                    errores.add("Error en fila " + fila + ": " + e.getMessage());
                }

                fila++;
            }

            workbook.close();

        } catch (Exception e) {
            return new DTOResultadoCarga(0, 0, 0,
                    List.of("No se pudo leer el Excel: " + e.getMessage()));
        }

        return new DTOResultadoCarga(productos.size(), productos.size(), errores.size(), errores);
    }

    private String getValue(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            default -> "";
        };
    }
}
