package com.example.MarketS.controller.api;

import com.example.MarketS.dto.ProductoResponseDTO;
import com.example.MarketS.dto.SugerenciaResponseDTO;
import com.example.MarketS.model.Producto;
import com.example.MarketS.repository.CategoriaRepository;
import com.example.MarketS.repository.DetallePedidoRepository;
import com.example.MarketS.repository.MarcaRepository;
import com.example.MarketS.repository.ProductoRepository;
import com.example.MarketS.repository.ResenaRepository;
import com.example.MarketS.repository.UserRepository;
import com.example.MarketS.service.ReputacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProductoApiController {

    // Estados que representan una venta concretada, sea por MercadoPago o por fuera de la app
    // (mismo criterio que VendedorApiController/AdminApiController/VendedorPublicController):
    // RECIBIDO siempre viene después de PAGADO —el estado se sobreescribe, no se acumula— así
    // que filtrar solo por PAGADO subcontaba "unidades vendidas" apenas el comprador confirmaba
    // recepción, y dejaba afuera del todo a las ventas concretadas por fuera de la app.
    private static final List<String> ESTADOS_VENTA_CONCRETADA = List.of("PAGADO", "RECIBIDO", "COMPLETADO_EXTERNO");

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final ResenaRepository resenaRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final UserRepository userRepository;
    private final ReputacionService reputacionService;

    public ProductoApiController(ProductoRepository productoRepository,
                                  CategoriaRepository categoriaRepository,
                                  MarcaRepository marcaRepository,
                                  ResenaRepository resenaRepository,
                                  DetallePedidoRepository detallePedidoRepository,
                                  UserRepository userRepository,
                                  ReputacionService reputacionService) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.resenaRepository = resenaRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.userRepository = userRepository;
        this.reputacionService = reputacionService;
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

        return ResponseEntity.ok(buildDTOs(productos));
    }

    // GET /api/productos/recientes?limit=10 — últimos productos agregados (orden descendente).
    // limit<=0 devuelve el listado completo, usado por la vista "Ver todos los últimos productos".
    @GetMapping("/productos/recientes")
    public ResponseEntity<List<ProductoResponseDTO>> productosRecientes(
            @RequestParam(defaultValue = "10") int limit) {
        List<Producto> productos = limit > 0
                ? productoRepository.findByActivoTrueOrderByFechaAgregacionDesc(
                        org.springframework.data.domain.PageRequest.of(0, limit))
                : productoRepository.findByActivoTrueOrderByFechaAgregacionDesc();
        return ResponseEntity.ok(buildDTOs(productos));
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

    // GET /api/productos/{id} — detalle de producto (1 producto = consultas individuales, no hay N+1)
    @GetMapping("/productos/{id}")
    public ResponseEntity<ProductoResponseDTO> detalle(@PathVariable Long id) {
        return productoRepository.findById(id)
                .filter(Producto::getActivo)
                .map(p -> {
                    // p.getVendedor() es LAZY y ya quedó fuera de sesión en este punto (el
                    // findById corrió en su propia transacción); se resuelve el username aparte.
                    Long vendedorId = p.getVendedor() != null ? p.getVendedor().getId() : null;
                    Map<Long, String> vendedorUsername = vendedorId != null
                            ? userRepository.findIdAndUsernameByIdIn(List.of(vendedorId)).stream()
                                    .collect(java.util.stream.Collectors.toMap(
                                            row -> ((Number) row[0]).longValue(), row -> (String) row[1]))
                            : Map.of();
                    return ProductoResponseDTO.from(
                            p,
                            resenaRepository.avgCalificacionByProductoId(p.getId()),
                            resenaRepository.countByProductoId(p.getId()),
                            detallePedidoRepository.sumCantidadVendidaByProductoId(p.getId(), ESTADOS_VENTA_CONCRETADA),
                            vendedorUsername,
                            vendedorId != null ? reputacionService.calcularVendedor(vendedorId) : null);
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/productos/categoria/{categoriaId} — filtro por categoría
    @GetMapping("/productos/categoria/{categoriaId}")
    public ResponseEntity<List<ProductoResponseDTO>> porCategoria(@PathVariable Long categoriaId) {
        List<Producto> productos = productoRepository.findByCategoriasIdAndActivoTrue(categoriaId);
        return ResponseEntity.ok(buildDTOs(productos));
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

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /**
     * Convierte una lista de productos a DTOs enriquecidos con estadísticas
     * usando solo 3 queries batch (independientemente de cuántos productos haya).
     */
    private List<ProductoResponseDTO> buildDTOs(List<Producto> productos) {
        if (productos.isEmpty()) return List.of();

        List<Long> ids = productos.stream().map(Producto::getId).toList();

        // 1 query para promedios y conteo de reseñas
        Map<Long, double[]> statsResenas = new HashMap<>();
        resenaRepository.findEstadisticasByProductoIds(ids).forEach(row -> {
            Long pid     = ((Number) row[0]).longValue();
            double avg   = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            long   count = ((Number) row[2]).longValue();
            statsResenas.put(pid, new double[]{ avg, count });
        });

        // 1 query para total vendido
        Map<Long, Long> statsVentas = new HashMap<>();
        detallePedidoRepository.findTotalVendidoByProductoIds(ids, ESTADOS_VENTA_CONCRETADA).forEach(row -> {
            Long pid   = ((Number) row[0]).longValue();
            long total = ((Number) row[1]).longValue();
            statsVentas.put(pid, total);
        });

        // 1 query para usernames de vendedores (evita cargar el User completo por producto,
        // lo que dispararía una consulta extra a "vendedores" por cada uno — ver Producto.vendedor)
        List<Long> vendedorIds = productos.stream()
                .map(p -> p.getVendedor() != null ? p.getVendedor().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> vendedorUsernames = new HashMap<>();
        if (!vendedorIds.isEmpty()) {
            userRepository.findIdAndUsernameByIdIn(vendedorIds).forEach(row ->
                    vendedorUsernames.put(((Number) row[0]).longValue(), (String) row[1]));
        }

        return productos.stream().map(p -> {
            double[] resena = statsResenas.getOrDefault(p.getId(), new double[]{ 0.0, 0 });
            long vendido    = statsVentas.getOrDefault(p.getId(), 0L);
            return ProductoResponseDTO.from(
                    p,
                    resena[0] > 0 ? resena[0] : null,
                    (long) resena[1],
                    vendido,
                    vendedorUsernames
            );
        }).toList();
    }
}
