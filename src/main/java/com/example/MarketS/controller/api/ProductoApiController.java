package com.example.MarketS.controller.api;

import com.example.MarketS.dto.ProductoResponseDTO;
import com.example.MarketS.dto.SugerenciaResponseDTO;
import com.example.MarketS.model.Categorias;
import com.example.MarketS.model.Producto;
import com.example.MarketS.repository.CategoriaRepository;
import com.example.MarketS.repository.MarcaRepository;
import com.example.MarketS.repository.ProductoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProductoApiController {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;

    public ProductoApiController(ProductoRepository productoRepository,
                                  CategoriaRepository categoriaRepository,
                                  MarcaRepository marcaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
    }

    // GET /api/productos — catálogo principal (aleatoriamente limitado a 30)
    @GetMapping("/productos")
    public ResponseEntity<List<ProductoResponseDTO>> listarProductos(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "30") int limit) {

        List<Producto> productos;

        if (q != null && !q.isBlank()) {
            productos = productoRepository.findByNombreContainingIgnoreCase(q)
                    .stream().filter(Producto::getActivo).toList();
        } else {
            productos = productoRepository.findByActivoTrue();
            Collections.shuffle(productos);
            if (limit > 0 && productos.size() > limit) {
                productos = productos.subList(0, limit);
            }
        }

        List<ProductoResponseDTO> respuesta = productos.stream()
                .map(ProductoResponseDTO::from)
                .toList();

        return ResponseEntity.ok(respuesta);
    }

    // GET /api/productos/sugerencias?q=... — autocompletado (máx 8 resultados livianos)
    @GetMapping("/productos/sugerencias")
    public ResponseEntity<List<SugerenciaResponseDTO>> sugerencias(
            @RequestParam(required = false, defaultValue = "") String q) {
        if (q.isBlank() || q.length() < 2) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<SugerenciaResponseDTO> resultado = productoRepository
                .findByNombreContainingIgnoreCase(q)
                .stream()
                .filter(Producto::getActivo)
                .limit(8)
                .map(p -> new SugerenciaResponseDTO(
                        p.getId(),
                        p.getNombre(),
                        p.getPrecio(),
                        p.getImagen(),
                        p.getCategorias() != null ? p.getCategorias().getNombreCategoria() : null
                ))
                .toList();
        return ResponseEntity.ok(resultado);
    }

    // GET /api/productos/{id} — detalle de producto
    @GetMapping("/productos/{id}")
    public ResponseEntity<ProductoResponseDTO> detalle(@PathVariable Long id) {
        return productoRepository.findById(id)
                .filter(Producto::getActivo)
                .map(ProductoResponseDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/productos/categoria/{categoriaId} — filtro por categoría
    @GetMapping("/productos/categoria/{categoriaId}")
    public ResponseEntity<List<ProductoResponseDTO>> porCategoria(@PathVariable Long categoriaId) {
        List<ProductoResponseDTO> resultado = productoRepository
                .findByCategoriasIdAndActivoTrue(categoriaId)
                .stream()
                .map(ProductoResponseDTO::from)
                .toList();
        return ResponseEntity.ok(resultado);
    }

    // GET /api/categorias — listado completo de categorías
    @GetMapping("/categorias")
    public ResponseEntity<List<Map<String, Object>>> listarCategorias() {
        List<Map<String, Object>> categorias = categoriaRepository.findAll()
                .stream()
                .map(c -> Map.<String, Object>of("id", c.getId(), "nombre", c.getNombreCategoria()))
                .toList();
        return ResponseEntity.ok(categorias);
    }

    // GET /api/marcas — listado completo de marcas
    @GetMapping("/marcas")
    public ResponseEntity<List<Map<String, Object>>> listarMarcas() {
        List<Map<String, Object>> marcas = marcaRepository.findAll()
                .stream()
                .map(m -> Map.<String, Object>of("id", m.getId(), "nombre", m.getNombreMarca()))
                .toList();
        return ResponseEntity.ok(marcas);
    }
}
