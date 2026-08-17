package com.example.MarketS.service;

import com.example.MarketS.repository.FavoritoRepository;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.repository.ProductoRepository;
import com.example.MarketS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Estadísticas de dashboard compartidas entre AdminApiController y ModeradorApiController:
// ambos mostraban el mismo subconjunto base (productos, pedidos, vendedores, usuarios,
// favoritos, producto más guardado), con productoMasGuardado() literalmente duplicado en
// los dos controllers.
@Service
@RequiredArgsConstructor
public class DashboardStatsService {

    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;
    private final UserRepository userRepository;
    private final FavoritoRepository favoritoRepository;

    // Subconjunto de estadísticas que necesitan tanto el dashboard de admin como el de
    // moderador. Admin le agrega encima sus propias métricas (desglose por rol, reputación) —
    // ver AdminApiController.dashboard().
    public Map<String, Object> estadisticasBase() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalProductos", productoRepository.count());
        stats.put("productosActivos", productoRepository.countByActivoTrue());
        stats.put("totalPedidos", pedidoRepository.count());
        stats.put("totalVendedores", userRepository.countByRolRolname("ROLE_VENDEDOR"));
        stats.put("totalUsuarios", userRepository.count());
        stats.put("totalFavoritos", favoritoRepository.count());
        stats.put("productoMasGuardado", productoMasGuardado());
        return stats;
    }

    // Producto con más favoritos guardados en toda la plataforma. Map.of(...) no admite valores
    // null, así que si todavía nadie ha guardado nada se devuelve un mapa vacío en vez de null
    // (el frontend lo trata igual: ausencia de "id" = sin datos aún).
    public Map<String, Object> productoMasGuardado() {
        List<Object[]> top = favoritoRepository.findProductosMasGuardados(PageRequest.of(0, 1));
        if (top.isEmpty()) return Map.of();
        Object[] row = top.get(0);
        return Map.of(
                "id", ((Number) row[0]).longValue(),
                "nombre", (String) row[1],
                "totalFavoritos", ((Number) row[2]).longValue()
        );
    }
}
