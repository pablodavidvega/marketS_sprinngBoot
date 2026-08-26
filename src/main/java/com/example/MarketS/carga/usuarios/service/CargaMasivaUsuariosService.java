package com.example.MarketS.carga.usuarios.service;

import com.example.MarketS.carga.dto.DTOResultadoCarga;
import com.example.MarketS.model.Rol;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.RolRepository;
import com.example.MarketS.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CargaMasivaUsuariosService {

    private final UserRepository userRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    // Fila ya parseada en un User candidato, pendiente de la validación de duplicados en batch.
    private record Candidato(int fila, User usuario) {}

    @Transactional
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
        List<Candidato> candidatos = new ArrayList<>();
        int total = 0;

        Map<String, Rol> rolesPorNombre = precargarRoles();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {

            String linea;
            int fila = 0;
            while ((linea = br.readLine()) != null) {
                if (fila == 0) { fila++; continue; } // saltar cabecera
                if (linea.isBlank()) { fila++; continue; }
                total++;
                try {
                    String[] datos = parseLineaCsv(linea);
                    candidatos.add(new Candidato(fila, construirUsuario(datos, rolesPorNombre)));
                } catch (Exception e) {
                    errores.add("Fila " + fila + ": " + e.getMessage());
                }
                fila++;
            }
        } catch (Exception e) {
            return new DTOResultadoCarga(0, 0, 0, List.of("Error al leer el CSV: " + e.getMessage()));
        }

        int correctos = guardarValidos(candidatos, errores);
        return new DTOResultadoCarga(total, correctos, total - correctos, errores);
    }

    // ── Plantilla Excel ──────────────────────────────────────────────────────
    // Mismas columnas y valores de ejemplo que la plantilla CSV del frontend
    // (ver COLUMNAS en CargaMasivaUsuariosPage.tsx) — listas para que el admin
    // borre la fila de ejemplo y pegue sus propios datos.

    private static final String[] PLANTILLA_HEADERS = {"username", "email", "password", "phone", "address", "rol"};
    private static final String[] PLANTILLA_EJEMPLO  = {"juan.perez", "juan@correo.com", "Clave2024!", "3001234567", "Calle 10 # 5-20", "CLIENTE"};

    public void generarPlantillaExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=plantilla_usuarios.xlsx");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Usuarios");

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < PLANTILLA_HEADERS.length; i++) {
                headerRow.createCell(i).setCellValue(PLANTILLA_HEADERS[i]);
            }

            Row ejemploRow = sheet.createRow(1);
            for (int i = 0; i < PLANTILLA_EJEMPLO.length; i++) {
                ejemploRow.createCell(i).setCellValue(PLANTILLA_EJEMPLO[i]);
            }

            for (int i = 0; i < PLANTILLA_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Error al generar la plantilla Excel", e);
        }
    }

    // ── Excel ─────────────────────────────────────────────────────────────────

    private DTOResultadoCarga procesarExcel(MultipartFile archivo) {
        List<String> errores = new ArrayList<>();
        List<Candidato> candidatos = new ArrayList<>();
        int total = 0;

        Map<String, Rol> rolesPorNombre = precargarRoles();

        try (Workbook workbook = WorkbookFactory.create(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int fila = 0;
            for (Row row : sheet) {
                if (fila == 0) { fila++; continue; } // saltar cabecera
                if (isFilaVacia(row)) { fila++; continue; }
                total++;
                try {
                    String[] datos = new String[6];
                    for (int i = 0; i < 6; i++) datos[i] = getCeldaValor(row, i);
                    candidatos.add(new Candidato(fila, construirUsuario(datos, rolesPorNombre)));
                } catch (Exception e) {
                    errores.add("Fila " + fila + ": " + e.getMessage());
                }
                fila++;
            }
        } catch (Exception e) {
            return new DTOResultadoCarga(0, 0, 0, List.of("Error al leer el Excel: " + e.getMessage()));
        }

        int correctos = guardarValidos(candidatos, errores);
        return new DTOResultadoCarga(total, correctos, total - correctos, errores);
    }

    // ── Precarga / dedupe / guardado en batch ───────────────────────────────────

    // Los roles son un puñado de filas fijas (CLIENTE/VENDEDOR/...): se cargan una sola vez en
    // vez de una query por fila (antes: rolRepository.findByRolname por cada usuario del archivo).
    private Map<String, Rol> precargarRoles() {
        Map<String, Rol> roles = new HashMap<>();
        rolRepository.findAll().forEach(r -> roles.put(r.getRolname(), r));
        return roles;
    }

    // Descarta duplicados (contra la BD y entre filas del propio archivo) con 2 queries en vez
    // de 2 por fila, y guarda todo lo válido en un solo saveAll en vez de un save por fila.
    private int guardarValidos(List<Candidato> candidatos, List<String> errores) {
        if (candidatos.isEmpty()) return 0;

        Set<String> usernames = candidatos.stream().map(c -> c.usuario().getUsername())
                .collect(java.util.stream.Collectors.toSet());
        Set<String> emails = candidatos.stream().map(c -> c.usuario().getEmail())
                .collect(java.util.stream.Collectors.toSet());
        Set<String> usernamesExistentes = new HashSet<>(userRepository.findExistingUsernames(usernames));
        Set<String> emailsExistentes = new HashSet<>(userRepository.findExistingEmails(emails));

        List<User> validos = new ArrayList<>();
        Set<String> usernamesEnLote = new HashSet<>();
        Set<String> emailsEnLote = new HashSet<>();

        for (Candidato c : candidatos) {
            User u = c.usuario();
            if (usernamesExistentes.contains(u.getUsername()) || !usernamesEnLote.add(u.getUsername())) {
                errores.add("Fila " + c.fila() + ": username '" + u.getUsername() + "' ya existe");
                continue;
            }
            if (emailsExistentes.contains(u.getEmail()) || !emailsEnLote.add(u.getEmail())) {
                errores.add("Fila " + c.fila() + ": email '" + u.getEmail() + "' ya existe");
                continue;
            }
            u.setPassword(passwordEncoder.encode(u.getPassword()));
            u.setCreatedAt(LocalDateTime.now());
            u.setUpdatedAt(LocalDateTime.now());
            validos.add(u);
        }

        if (validos.isEmpty()) return 0;

        try {
            userRepository.saveAll(validos);
            return validos.size();
        } catch (Exception e) {
            // Caso raro (ej. choque con un registro creado justo entre la validación y el
            // guardado): se reporta como fallo de lote en vez de dejar un 500 sin explicación.
            errores.add("Error al guardar el lote en la base de datos: " + e.getMessage());
            return 0;
        }
    }

    // ── Construcción del usuario ──────────────────────────────────────────────

    private User construirUsuario(String[] datos, Map<String, Rol> rolesPorNombre) {
        String username = col(datos, 0);
        String email    = col(datos, 1);
        String password = col(datos, 2);
        String phone    = col(datos, 3);

        if (username.isBlank()) throw new IllegalArgumentException("'username' es obligatorio");
        if (email.isBlank())    throw new IllegalArgumentException("'email' es obligatorio");
        if (password.isBlank()) throw new IllegalArgumentException("'password' es obligatoria");

        // username, email, password y phone son siempre las primeras 4 columnas, y rol siempre
        // la última. Si el CSV no encerró entre comillas una dirección con coma (ej. "Calle 10 #
        // 5-20, Bogotá"), la coma interna la parte en columnas de más: todo lo que quede entre
        // phone y la última columna se reconstruye como una sola dirección, en vez de que el
        // rol quede empujado a un valor que nunca coincide con VENDEDOR/CLIENTE.
        String address;
        String rolInput;
        if (datos.length <= 6) {
            address = col(datos, 4);
            rolInput = col(datos, 5).toUpperCase();
        } else {
            StringBuilder direccion = new StringBuilder();
            for (int i = 4; i < datos.length - 1; i++) {
                if (direccion.length() > 0) direccion.append(", ");
                direccion.append(col(datos, i));
            }
            address = direccion.toString();
            rolInput = col(datos, datos.length - 1).toUpperCase();
        }

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword(password); // se encripta en guardarValidos, una vez que pasó la dedupe
        u.setPhone(phone.isBlank() ? "" : phone);
        u.setAddress(address.isBlank() ? null : address);
        u.setActive(true);

        // Rol: CLIENTE por defecto, acepta CLIENTE o VENDEDOR
        String rolname = rolInput.equals("VENDEDOR") ? "ROLE_VENDEDOR" : "ROLE_CLIENTE";
        Rol rol = rolesPorNombre.get(rolname);
        if (rol == null) throw new IllegalStateException("Rol '" + rolname + "' no encontrado en la BD");
        u.setRol(rol);

        return u;
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
