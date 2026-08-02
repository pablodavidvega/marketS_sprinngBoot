package com.example.MarketS.controller.api;

import com.example.MarketS.dto.PedidoModeradorDTO;
import com.example.MarketS.dto.ProductoResponseDTO;
import com.example.MarketS.model.Producto;
import com.example.MarketS.repository.FavoritoRepository;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.repository.ProductoRepository;
import com.example.MarketS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/moderador")
@PreAuthorize("hasAnyAuthority('ROLE_MODERADOR', 'ROLE_ADMIN')")
@RequiredArgsConstructor
public class ModeradorApiController {

    private final ProductoRepository productoRepository;
    private final PedidoRepository   pedidoRepository;
    private final UserRepository      userRepository;
    private final FavoritoRepository  favoritoRepository;

    // GET /api/moderador/dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        long totalProductos   = productoRepository.count();
        long productosActivos = productoRepository.countByActivoTrue();
        long totalPedidos     = pedidoRepository.count();
        long totalVendedores  = userRepository.countByRolRolname("ROLE_VENDEDOR");
        long totalUsuarios    = userRepository.count();

        return ResponseEntity.ok(Map.of(
            "totalProductos",   totalProductos,
            "productosActivos", productosActivos,
            "totalPedidos",     totalPedidos,
            "totalVendedores",  totalVendedores,
            "totalUsuarios",    totalUsuarios,
            "totalFavoritos",      favoritoRepository.count(),
            "productoMasGuardado", productoMasGuardado()
        ));
    }

    // Producto con más favoritos guardados en toda la plataforma. Map.of(...) no admite valores
    // null, así que si todavía nadie ha guardado nada se devuelve un mapa vacío en vez de null
    // (el frontend lo trata igual: ausencia de "id" = sin datos aún).
    private Map<String, Object> productoMasGuardado() {
        List<Object[]> top = favoritoRepository.findProductosMasGuardados(PageRequest.of(0, 1));
        if (top.isEmpty()) return Map.of();
        Object[] row = top.get(0);
        return Map.of(
                "id", ((Number) row[0]).longValue(),
                "nombre", (String) row[1],
                "totalFavoritos", ((Number) row[2]).longValue()
        );
    }

    // GET /api/moderador/productos — productos paginados (activos e inactivos), con filtro
    // opcional de activo/inactivo y búsqueda por nombre, vendedor o categoría.
    // Resuelve los usernames de vendedor en batch (1 query) en vez de dejar que
    // ProductoResponseDTO.from(p) toque p.getVendedor().getUsername() por cada producto,
    // que dispararía una consulta extra por fila (ver mismo patrón en VendedorApiController).
    @GetMapping("/productos")
    public ResponseEntity<Map<String, Object>> productos(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Page<Producto> pagina = productoRepository.findAllFiltrado(
                activo,
                (q == null || q.isBlank()) ? null : q,
                PageRequest.of(page, size));

        List<Long> vendedorIds = pagina.getContent().stream()
                .map(p -> p.getVendedor() != null ? p.getVendedor().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> vendedorUsernames = new HashMap<>();
        if (!vendedorIds.isEmpty()) {
            userRepository.findIdAndUsernameByIdIn(vendedorIds).forEach(row ->
                    vendedorUsernames.put(((Number) row[0]).longValue(), (String) row[1]));
        }
        List<ProductoResponseDTO> contenido = pagina.getContent().stream()
                .map(p -> ProductoResponseDTO.from(p, null, null, null, vendedorUsernames))
                .toList();

        return ResponseEntity.ok(Map.of(
                "contenido", contenido,
                "paginaActual", pagina.getNumber(),
                "totalPaginas", pagina.getTotalPages(),
                "totalElementos", pagina.getTotalElements(),
                "totalActivos", productoRepository.countByActivoTrue(),
                "totalInactivos", productoRepository.countByActivoFalse()
        ));
    }

    // PATCH /api/moderador/productos/{id}/toggle — activar o desactivar
    @PatchMapping("/productos/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleActivo(@PathVariable Long id) {
        return productoRepository.findById(id)
                .map(p -> {
                    p.setActivo(!p.getActivo());
                    productoRepository.save(p);
                    return ResponseEntity.ok(Map.<String, Object>of("id", p.getId(), "activo", p.getActivo()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/moderador/pedidos — pedidos paginados, con filtro opcional de estado y búsqueda
    // por comprador/ID. Antes traía TODOS los pedidos de la plataforma en una sola llamada
    // (~37s con el catálogo actual) — ver comentario en PedidoRepository.findAllWithUsuarioFiltrado.
    @GetMapping("/pedidos")
    public ResponseEntity<Map<String, Object>> pedidos(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Page<PedidoModeradorDTO> pagina = pedidoRepository.findAllWithUsuarioFiltrado(
                (estado == null || estado.isBlank()) ? null : estado,
                (q == null || q.isBlank()) ? null : q,
                PageRequest.of(page, size));

        List<Map<String, Object>> contenido = pagina.getContent().stream()
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",        p.id());
                    m.put("comprador", p.comprador());
                    m.put("fecha",     p.fecha().toString());
                    m.put("estado",    p.estado());
                    m.put("total",     p.total());
                    return m;
                })
                .toList();

        return ResponseEntity.ok(Map.of(
                "contenido", contenido,
                "paginaActual", pagina.getNumber(),
                "totalPaginas", pagina.getTotalPages(),
                "totalElementos", pagina.getTotalElements()
        ));
    }
}
