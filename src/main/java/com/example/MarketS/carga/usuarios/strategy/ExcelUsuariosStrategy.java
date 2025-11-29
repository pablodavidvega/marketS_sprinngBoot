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
                if (fila == 1) continue; // Saltar cabecera

                try {
                    User u = new User();

                    u.setUsername(getStringValue(row, 1));
                    u.setEmail(getStringValue(row, 2));
                    u.setPassword(getStringValue(row, 3));
                    u.setPhone(getStringValue(row, 4));
                    u.setAddress(getStringValue(row, 5));
                    u.setActive(convertToBoolean(getStringValue(row, 6)));

                    // Validaciones básicas DURANTE la lectura
                    if (u.getUsername() == null || u.getUsername().trim().isEmpty()) {
                        resultado.getErrores().add("Fila " + fila + ": username vacío");
                        resultado.setFallidos(resultado.getFallidos() + 1);
                        continue; // No agregar a la lista
                    }

                    if (u.getEmail() == null || u.getEmail().trim().isEmpty()) {
                        resultado.getErrores().add("Fila " + fila + ": email vacío");
                        resultado.setFallidos(resultado.getFallidos() + 1);
                        continue; // No agregar a la lista
                    }

                    // SOLO si pasa todas las validaciones, agregar y contar
                    usuarios.add(u);
                    resultado.setProcesados(resultado.getProcesados() + 1);

                    // DEBUG
                    System.out.println("✓ Leído usuario: " + u.getUsername() + " | Active: " + u.isActive());

                } catch (Exception e) {
                    resultado.getErrores().add("Fila " + fila + " error: " + e.getMessage());
                    resultado.setFallidos(resultado.getFallidos() + 1);
                }
            }

        } catch (Exception e) {
            resultado.getErrores().add("Error leyendo Excel: " + e.getMessage());
        }

        System.out.println("DEBUG - Total leídos del Excel: " + usuarios.size());
        return usuarios;
    }

    private String getStringValue(Row row, int col) {
        if (row == null || row.getCell(col) == null) {
            return "";
        }

        Cell cell = row.getCell(col);
        try {
            // Manejar diferentes tipos de celdas
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    // Si es numérico pero queremos string (como phone)
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        // Convertir a string sin decimales si es entero
                        double numValue = cell.getNumericCellValue();
                        if (numValue == (long) numValue) {
                            return String.valueOf((long) numValue);
                        } else {
                            return String.valueOf(numValue);
                        }
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    // Evaluar fórmulas
                    switch (cell.getCachedFormulaResultType()) {
                        case STRING:
                            return cell.getStringCellValue();
                        case NUMERIC:
                            return String.valueOf(cell.getNumericCellValue());
                        case BOOLEAN:
                            return String.valueOf(cell.getBooleanCellValue());
                        default:
                            return cell.toString();
                    }
                default:
                    return cell.toString().trim();
            }
        } catch (Exception e) {
            return cell.toString().trim();
        }
    }

    private boolean convertToBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        String normalized = value.trim().toLowerCase();

        // Manejar diferentes formatos de booleanos
        return normalized.equals("1") ||
                normalized.equals("true") ||
                normalized.equals("yes") ||
                normalized.equals("si") ||
                normalized.equals("sí") ||
                normalized.equals("activo") ||
                normalized.equals("active");
    }
}