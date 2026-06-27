package com.example.MarketS.controller.api;

import com.example.MarketS.dto.ProductoResponseDTO;
import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.Producto;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.repository.ProductoRepository;
import com.example.MarketS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

    // GET /api/moderador/dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        long totalProductos   = productoRepository.count();
        long productosActivos = productoRepository.findByActivoTrue().size();
        long totalPedidos     = pedidoRepository.count();
        long totalVendedores  = userRepository.countByRolRolname("ROLE_VENDEDOR");
        long totalUsuarios    = userRepository.count();

        return ResponseEntity.ok(Map.of(
            "totalProductos",   totalProductos,
            "productosActivos", productosActivos,
            "totalPedidos",     totalPedidos,
            "totalVendedores",  totalVendedores,
            "totalUsuarios",    totalUsuarios
        ));
    }

    // GET /api/moderador/productos — todos los productos (activos e inactivos)
    @GetMapping("/productos")
    public ResponseEntity<List<ProductoResponseDTO>> productos() {
        List<ProductoResponseDTO> lista = productoRepository.findAll().stream()
                .map(ProductoResponseDTO::from)
                .toList();
        return ResponseEntity.ok(lista);
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

    // GET /api/moderador/pedidos — todos los pedidos
    @GetMapping("/pedidos")
    public ResponseEntity<List<Map<String, Object>>> pedidos() {
        List<Map<String, Object>> lista = pedidoRepository.findAllWithUsuario().stream()
                .map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",        p.getId());
                    m.put("comprador", p.getUsuario().getUsername());
                    m.put("fecha",     p.getFecha().toString());
                    m.put("estado",    p.getEstado());
                    m.put("total",     p.getTotal());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(lista);
    }
}
