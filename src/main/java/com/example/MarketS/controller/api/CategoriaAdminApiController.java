package com.example.MarketS.controller.api;

import com.example.MarketS.dto.CategoriaAdminDTO;
import com.example.MarketS.model.Categorias;
import com.example.MarketS.repository.CategoriaRepository;
import com.example.MarketS.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Gestión de categorías desde el panel de administrador.
// Las categorías creadas antes de esta funcionalidad conviven aquí como filas normales de la misma tabla.
@RestController
@RequestMapping("/api/admin/categorias")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class CategoriaAdminApiController {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaAdminApiController(CategoriaRepository categoriaRepository,
                                        ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    // GET /api/admin/categorias — listado completo con cantidad de productos por categoría
    @GetMapping
    public ResponseEntity<List<CategoriaAdminDTO>> listar() {
        List<CategoriaAdminDTO> categorias = categoriaRepository.findAll().stream()
                .map(c -> CategoriaAdminDTO.from(c, productoRepository.countByCategoriasId(c.getId())))
                .toList();
        return ResponseEntity.ok(categorias);
    }

    // POST /api/admin/categorias — crear categoría nueva
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, String> body) {
        String nombre = body.getOrDefault("nombreCategoria", "").trim();
        if (nombre.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre de la categoría es obligatorio"));
        }
        if (categoriaRepository.findByNombreCategoriaIgnoreCase(nombre).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ya existe una categoría con ese nombre"));
        }

        Categorias categoria = new Categorias();
        categoria.setNombreCategoria(nombre);
        Categorias guardada = categoriaRepository.save(categoria);
        return ResponseEntity.ok(CategoriaAdminDTO.from(guardada, 0));
    }

    // PUT /api/admin/categorias/{id} — renombrar categoría existente
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Categorias categoria = categoriaRepository.findById(id).orElse(null);
        if (categoria == null) {
            return ResponseEntity.notFound().build();
        }

        String nombre = body.getOrDefault("nombreCategoria", "").trim();
        if (nombre.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre de la categoría es obligatorio"));
        }
        boolean duplicado = categoriaRepository.findByNombreCategoriaIgnoreCase(nombre)
                .filter(existente -> !existente.getId().equals(id))
                .isPresent();
        if (duplicado) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ya existe una categoría con ese nombre"));
        }

        categoria.setNombreCategoria(nombre);
        Categorias guardada = categoriaRepository.save(categoria);
        return ResponseEntity.ok(CategoriaAdminDTO.from(guardada, productoRepository.countByCategoriasId(id)));
    }

    // DELETE /api/admin/categorias/{id} — elimina solo si no tiene productos asociados
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (categoriaRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        long totalProductos = productoRepository.countByCategoriasId(id);
        if (totalProductos > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "No se puede eliminar: hay " + totalProductos + " producto(s) asignado(s) a esta categoría"
            ));
        }
        categoriaRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("mensaje", "Categoría eliminada correctamente"));
    }
}
