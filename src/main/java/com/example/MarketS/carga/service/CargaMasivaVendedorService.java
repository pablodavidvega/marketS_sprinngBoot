package com.example.MarketS.carga.service;

import com.example.MarketS.carga.dto.DTOResultadoCarga;
import com.example.MarketS.model.Producto;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.CategoriaRepository;
import com.example.MarketS.repository.MarcaRepository;
import com.example.MarketS.repository.ProductoRepository;
import com.example.MarketS.service.CloudStorageService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class CargaMasivaVendedorService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final CloudStorageService cloudStorageService;

    public DTOResultadoCarga procesar(MultipartFile archivo, MultipartFile zip, User vendedor) {

        Map<String, String> mediaMap = new HashMap<>();
        if (zip != null && !zip.isEmpty()) {
            try {
                mediaMap = procesarZip(zip);
            } catch (Exception e) {
                return new DTOResultadoCarga(0, 0, 0,
                        List.of("Error al procesar el ZIP: " + e.getMessage()));
            }
        }

        String nombreArchivo = archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename().toLowerCase() : "";

        if (nombreArchivo.endsWith(".csv") || nombreArchivo.endsWith(".txt")) {
            return procesarCsv(archivo, mediaMap, vendedor);
        } else if (nombreArchivo.endsWith(".xlsx") || nombreArchivo.endsWith(".xls")) {
            return procesarExcel(archivo, mediaMap, vendedor);
        } else {
            return new DTOResultadoCarga(0, 0, 0,
                    List.of("Formato no soportado. Use CSV (.csv) o Excel (.xlsx / .xls)"));
        }
    }

    private Map<String, String> procesarZip(MultipartFile zip) throws Exception {
        Map<String, String> mapa = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(zip.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) { zis.closeEntry(); continue; }
                String nombreEntrada = Paths.get(entry.getName()).getFileName().toString();
                byte[] bytes = zis.readAllBytes();
                String contentType = detectarContentType(nombreEntrada);
                String url = cloudStorageService.subirBytes(bytes, nombreEntrada, contentType);
                if (url != null) mapa.put(nombreEntrada, url);
                zis.closeEntry();
            }
        }
        return mapa;
    }

    // ── CSV ──────────────────────────────────────────────────────────────────

    private DTOResultadoCarga procesarCsv(MultipartFile archivo, Map<String, String> mediaMap, User vendedor) {
        List<String> errores = new ArrayList<>();
        int total = 0, correctos = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {

            String linea;
            int fila = 0;
            while ((linea = br.readLine()) != null) {
                if (fila == 0) { fila++; continue; }
                if (linea.isBlank()) { fila++; continue; }
                total++;
                try {
                    String[] datos = linea.split("[;,]", -1);
                    Producto p = construirProducto(datos, mediaMap, vendedor);
                    productoRepository.save(p);
                    correctos++;
                } catch (Exception e) {
                    errores.add("Fila " + fila + ": " + e.getMessage());
                }
                fila++;
            }
        } catch (Exception e) {
            return new DTOResultadoCarga(0, 0, 0, List.of("Error al leer el CSV: " + e.getMessage()));
        }

        return new DTOResultadoCarga(total, correctos, total - correctos, errores);
    }

    // ── Excel ─────────────────────────────────────────────────────────────────

    private DTOResultadoCarga procesarExcel(MultipartFile archivo, Map<String, String> mediaMap, User vendedor) {
        List<String> errores = new ArrayList<>();
        int total = 0, correctos = 0;

        try (Workbook workbook = WorkbookFactory.create(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int fila = 0;
            for (Row row : sheet) {
                if (fila == 0) { fila++; continue; }
                if (isFilaVacia(row)) { fila++; continue; }
                total++;
                try {
                    String[] datos = new String[10];
                    for (int i = 0; i < 10; i++) datos[i] = getCeldaValor(row, i);
                    Producto p = construirProducto(datos, mediaMap, vendedor);
                    productoRepository.save(p);
                    correctos++;
                } catch (Exception e) {
                    errores.add("Fila " + fila + ": " + e.getMessage());
                }
                fila++;
            }
        } catch (Exception e) {
            return new DTOResultadoCarga(0, 0, 0, List.of("Error al leer el Excel: " + e.getMessage()));
        }

        return new DTOResultadoCarga(total, correctos, total - correctos, errores);
    }

    // ── Construcción del producto ─────────────────────────────────────────────

    private Producto construirProducto(String[] datos, Map<String, String> mediaMap, User vendedor) {
        String nombre    = col(datos, 0);
        String precioStr = col(datos, 1);
        String desc      = col(datos, 2);
        String stockStr  = col(datos, 3);

        if (nombre.isBlank())    throw new IllegalArgumentException("'nombre' es obligatorio");
        if (precioStr.isBlank()) throw new IllegalArgumentException("'precio' es obligatorio");
        if (stockStr.isBlank())  throw new IllegalArgumentException("'stock' es obligatorio");

        Producto p = new Producto();
        p.setNombre(nombre);
        p.setPrecio(Double.parseDouble(precioStr.replace(",", ".")));
        p.setDescripcion(desc.isBlank() ? null : desc);
        p.setStock(Integer.parseInt(stockStr));
        p.setFechaAgregacion(new Date());
        p.setEstadoProducto("Disponible");
        p.setActivo(true);
        p.setVendedor(vendedor);

        String catNombre = col(datos, 4);
        if (!catNombre.isBlank())
            categoriaRepository.findByNombreCategoria(catNombre).ifPresent(p::setCategorias);

        String marcaNombre = col(datos, 5);
        if (!marcaNombre.isBlank())
            marcaRepository.findByNombreMarca(marcaNombre).ifPresent(p::setMarca);

        p.setImagen(resolverMedia(col(datos, 6), mediaMap));
        p.setImagen2(resolverMedia(col(datos, 7), mediaMap));
        p.setImagen3(resolverMedia(col(datos, 8), mediaMap));
        p.setVideo(resolverMedia(col(datos, 9), mediaMap));

        return p;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolverMedia(String nombreArchivo, Map<String, String> mediaMap) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) return null;
        return mediaMap.getOrDefault(nombreArchivo, null);
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
            default      -> "";
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

    private String detectarContentType(String nombre) {
        String lower = nombre.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".mp4"))  return "video/mp4";
        if (lower.endsWith(".mov"))  return "video/quicktime";
        if (lower.endsWith(".avi"))  return "video/x-msvideo";
        return "application/octet-stream";
    }
}
