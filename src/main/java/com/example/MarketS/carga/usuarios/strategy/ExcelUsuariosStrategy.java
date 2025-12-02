package com.example.MarketS.carga.usuarios.strategy;

import com.example.MarketS.carga.usuarios.dto.DTOResultadoCargaUsuarios;
import com.example.MarketS.model.User;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelUsuariosStrategy implements ArchivoUsuariosStrategy {

    @Override
    public List<User> leerArchivo(MultipartFile file, DTOResultadoCargaUsuarios resultado) {
        List<User> usuarios = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);
            int fila = 0;

            for (Row row : sheet) {
                fila++;

                // Saltar cabecera
                if (fila == 1) continue;

                try {
                    // Saltar filas vacías
                    if (row == null || row.getCell(1) == null) continue;

                    User u = new User();

                    // Igual que CSV: username es col 1, email col 2, password col 3...
                    u.setUsername(getStringValue(row, 1));
                    u.setEmail(getStringValue(row, 2));
                    u.setPassword(getStringValue(row, 3));
                    u.setPhone(getStringValue(row, 4));
                    u.setAddress(getStringValue(row, 5));

                    // active puede venir 1/0, true/false, activo, etc.
                    u.setActive(convertToBoolean(getStringValue(row, 6)));

                    // Validaciones mínimas
                    if (u.getUsername() == null || u.getUsername().trim().isEmpty()) {
                        resultado.getErrores().add("Fila " + fila + ": username vacío");
                        resultado.setFallidos(resultado.getFallidos() + 1);
                        continue;
                    }

                    if (u.getEmail() == null || u.getEmail().trim().isEmpty()) {
                        resultado.getErrores().add("Fila " + fila + ": email vacío");
                        resultado.setFallidos(resultado.getFallidos() + 1);
                        continue;
                    }

                    // SIEMPRE agregar aunque active sea 0
                    usuarios.add(u);

                    // Igual que CSV: sumar procesados siempre
                    resultado.setProcesados(resultado.getProcesados() + 1);

                } catch (Exception e) {
                    resultado.getErrores().add("Fila " + fila + " error: " + e.getMessage());
                    resultado.setFallidos(resultado.getFallidos() + 1);
                }
            }

        } catch (Exception e) {
            resultado.getErrores().add("Error leyendo Excel: " + e.getMessage());
        }

        return usuarios;
    }

    private String getStringValue(Row row, int col) {
        try {
            Cell cell = row.getCell(col);
            if (cell == null) return "";

            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    double num = cell.getNumericCellValue();
                    if (num == (long) num) {
                        return String.valueOf((long) num);
                    } else {
                        return String.valueOf(num);
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    return cell.toString().trim();
                default:
                    return cell.toString().trim();
            }
        } catch (Exception e) {
            return "";
        }
    }

    private boolean convertToBoolean(String value) {
        if (value == null) return false;

        String v = value.trim().toLowerCase();

        return v.equals("1") ||
                v.equals("true") ||
                v.equals("si") ||
                v.equals("sí") ||
                v.equals("yes") ||
                v.equals("active") ||
                v.equals("activo");
    }
}
