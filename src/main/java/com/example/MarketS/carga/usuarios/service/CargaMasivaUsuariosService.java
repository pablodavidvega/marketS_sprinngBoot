package com.example.MarketS.carga.usuarios.service;

import com.example.MarketS.carga.dto.DTOResultadoCarga;
import com.example.MarketS.model.Rol;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.RolRepository;
import com.example.MarketS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CargaMasivaUsuariosService {

    private final UserRepository userRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public DTOResultadoCarga procesar(MultipartFile archivo) {

        String nombreArchivo = archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename().toLowerCase() : "";

        if (nombreArchivo.endsWith(".csv")) {
            return procesarCsv(archivo);
        } else if (nombreArchivo.endsWith(".xlsx") || nombreArchivo.endsWith(".xls")) {
            return procesarExcel(archivo);
        } else {
            return new DTOResultadoCarga(0, 0, 0,
                    List.of("Formato no soportado. Use CSV (.csv) o Excel (.xlsx / .xls)"));
        }
    }

    // ── CSV ──────────────────────────────────────────────────────────────────

    private DTOResultadoCarga procesarCsv(MultipartFile archivo) {
        List<String> errores = new ArrayList<>();
        int total = 0, correctos = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {

            String linea;
            int fila = 0;
            while ((linea = br.readLine()) != null) {
                if (fila == 0) { fila++; continue; } // saltar cabecera
                if (linea.isBlank()) { fila++; continue; }
                total++;
                final int filaActual = fila;
                try {
                    String[] datos = parseLineaCsv(linea);
                    User u = construirUsuario(datos, filaActual);
                    guardarUsuario(u, filaActual, errores);
                    if (errores.stream().noneMatch(e -> e.startsWith("Fila " + filaActual))) correctos++;
                } catch (Exception e) {
                    errores.add("Fila " + filaActual + ": " + e.getMessage());
                }
                fila++;
            }
        } catch (Exception e) {
            return new DTOResultadoCarga(0, 0, 0, List.of("Error al leer el CSV: " + e.getMessage()));
        }

        int fallidos = total - correctos;
        return new DTOResultadoCarga(total, correctos, fallidos, errores);
    }

    // ── Excel ─────────────────────────────────────────────────────────────────

    private DTOResultadoCarga procesarExcel(MultipartFile archivo) {
        List<String> errores = new ArrayList<>();
        int total = 0, correctos = 0;

        try (Workbook workbook = WorkbookFactory.create(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int fila = 0;
            for (Row row : sheet) {
                if (fila == 0) { fila++; continue; } // saltar cabecera
                if (isFilaVacia(row)) { fila++; continue; }
                total++;
                final int filaActual = fila;
                try {
                    String[] datos = new String[6];
                    for (int i = 0; i < 6; i++) datos[i] = getCeldaValor(row, i);
                    User u = construirUsuario(datos, filaActual);
                    guardarUsuario(u, filaActual, errores);
                    if (errores.stream().noneMatch(e -> e.startsWith("Fila " + filaActual))) correctos++;
                } catch (Exception e) {
                    errores.add("Fila " + filaActual + ": " + e.getMessage());
                }
                fila++;
            }
        } catch (Exception e) {
            return new DTOResultadoCarga(0, 0, 0, List.of("Error al leer el Excel: " + e.getMessage()));
        }

        int fallidos = total - correctos;
        return new DTOResultadoCarga(total, correctos, fallidos, errores);
    }

    // ── Construcción del usuario ──────────────────────────────────────────────

    private User construirUsuario(String[] datos, int fila) {
        String username = col(datos, 0);
        String email    = col(datos, 1);
        String password = col(datos, 2);

        if (username.isBlank()) throw new IllegalArgumentException("'username' es obligatorio");
        if (email.isBlank())    throw new IllegalArgumentException("'email' es obligatorio");
        if (password.isBlank()) throw new IllegalArgumentException("'password' es obligatoria");

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword(password); // se encripta en guardarUsuario
        u.setPhone(col(datos, 3).isBlank() ? "" : col(datos, 3));
        u.setAddress(col(datos, 4).isBlank() ? null : col(datos, 4));
        u.setActive(true);

        // Rol: CLIENTE por defecto, acepta CLIENTE o VENDEDOR
        String rolInput = col(datos, 5).toUpperCase();
        String rolname = rolInput.equals("VENDEDOR") ? "ROLE_VENDEDOR" : "ROLE_CLIENTE";
        Rol rol = rolRepository.findByRolname(rolname)
                .orElseThrow(() -> new IllegalStateException("Rol '" + rolname + "' no encontrado en la BD"));
        u.setRol(rol);

        return u;
    }

    private void guardarUsuario(User u, int fila, List<String> errores) {
        if (userRepository.existsByUsername(u.getUsername())) {
            errores.add("Fila " + fila + ": username '" + u.getUsername() + "' ya existe");
            return;
        }
        if (userRepository.existsByEmail(u.getEmail())) {
            errores.add("Fila " + fila + ": email '" + u.getEmail() + "' ya existe");
            return;
        }
        u.setPassword(passwordEncoder.encode(u.getPassword()));
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        userRepository.save(u);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // Divide una línea de CSV en columnas respetando campos entre comillas dobles: una
    // dirección como "Calle 10 # 5-20, Barrio Centro" no debe partirse por la coma interna.
    // Acepta coma o punto y coma como separador (fuera de comillas), igual que antes.
    private String[] parseLineaCsv(String linea) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;

        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (!entreComillas && (c == ',' || c == ';')) {
                campos.add(actual.toString().trim());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString().trim());
        return campos.toArray(new String[0]);
    }

    private String col(String[] datos, int i) {
        if (i >= datos.length || datos[i] == null) return "";
        return datos[i].trim();
    }

    private String getCeldaValor(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                yield val == Math.floor(val) ? String.valueOf((long) val) : String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> cell.toString().trim();
        };
    }

    private boolean isFilaVacia(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && !cell.toString().isBlank()) return false;
        }
        return true;
    }
}
