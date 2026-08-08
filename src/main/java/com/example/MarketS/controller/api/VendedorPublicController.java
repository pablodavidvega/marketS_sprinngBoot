package com.example.MarketS.controller.api;

import com.example.MarketS.dto.ProductoResponseDTO;
import com.example.MarketS.model.Producto;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.DetallePedidoRepository;
import com.example.MarketS.repository.EnlaceSocialRepository;
import com.example.MarketS.repository.ProductoRepository;
import com.example.MarketS.repository.ResenaRepository;
import com.example.MarketS.repository.UserRepository;
import com.example.MarketS.service.ReputacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendedores")
@RequiredArgsConstructor
public class VendedorPublicController {

    // Estados que representan una venta concretada (RECIBIDO siempre viene después de PAGADO,
    // así que no hay doble conteo al incluir los tres).
    private static final List<String> ESTADOS_VENTA_CONCRETADA = List.of("PAGADO", "RECIBIDO", "COMPLETADO_EXTERNO");

    private final UserRepository         userRepository;
    private final ProductoRepository     productoRepository;
    private final ResenaRepository       resenaRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ReputacionService      reputacionService;
    private final EnlaceSocialRepository enlaceSocialRepository;

    // GET /api/vendedores/{id}/perfil — información pública del vendedor
    @GetMapping("/{id}/perfil")
    public ResponseEntity<Map<String, Object>> perfil(@PathVariable Long id) {
        return userRepository.findById(id)
                .filter(u -> u.isActive() && "ROLE_VENDEDOR".equals(u.getRol().getRolname()))
                .map(u -> {
                    long totalProductos = productoRepository.findByVendedorIdAndActivoTrue(u.getId()).size();
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("id",             u.getId());
                    resp.put("username",        u.getUsername());
                    resp.put("miembroDesde",    u.getCreatedAt());
                    resp.put("totalProductos",  totalProductos);
                    resp.put("city",            u.getVendedor() != null ? u.getVendedor().getCity() : null);
                    resp.put("reputacion",      reputacionService.calcularVendedor(u.getId()));
                    resp.put("totalVentas",     detallePedidoRepository.countPedidosDistinctByVendedorAndEstadoIn(u.getId(), ESTADOS_VENTA_CONCRETADA));
                    resp.put("unidadesVendidas", detallePedidoRepository.sumUnidadesByVendedorAndEstadoIn(u.getId(), ESTADOS_VENTA_CONCRETADA));
                    resp.put("enlaces", u.getVendedor() != null
                            ? enlaceSocialRepository.findByVendedorId(u.getVendedor().getId()).stream()
                                    .map(e -> Map.of("etiqueta", e.getEtiqueta(), "url", e.getUrl()))
                                    .toList()
                            : List.<Map<String, String>>of());
                    return ResponseEntity.ok(resp);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/vendedores/{id}/productos — catálogo público del vendedor, paginado real
    // (usado por VendedorPerfilPage con su botón "Ver más").
    @GetMapping("/{id}/productos")
    public ResponseEntity<Map<String, Object>> productos(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return userRepository.findById(id)
                .filter(u -> u.isActive())
                .map(u -> {
                    Page<Producto> pagina = productoRepository.paginaByVendedorIdAndActivoTrue(u.getId(), PageRequest.of(page, size));
                    Map<String, Object> respuesta = new HashMap<>();
                    respuesta.put("contenido", buildDTOs(pagina.getContent(), u));
                    respuesta.put("paginaActual", pagina.getNumber());
                    respuesta.put("totalPaginas", pagina.getTotalPages());
                    respuesta.put("totalElementos", pagina.getTotalElements());
                    return ResponseEntity.ok(respuesta);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private List<ProductoResponseDTO> buildDTOs(List<Producto> productos, User vendedor) {
        if (productos.isEmpty()) return List.of();

        List<Long> ids = productos.stream().map(Producto::getId).toList();

        Map<Long, double[]> statsResenas = new HashMap<>();
        resenaRepository.findEstadisticasByProductoIds(ids).forEach(row -> {
            Long pid   = ((Number) row[0]).longValue();
            double avg = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            long count = ((Number) row[2]).longValue();
            statsResenas.put(pid, new double[]{ avg, count });
        });

        Map<Long, Long> statsVentas = new HashMap<>();
        detallePedidoRepository.findTotalVendidoByProductoIds(ids, ESTADOS_VENTA_CONCRETADA).forEach(row -> {
            Long pid   = ((Number) row[0]).longValue();
            long total = ((Number) row[1]).longValue();
            statsVentas.put(pid, total);
        });

        // Todos estos productos son de "vendedor" (se filtró por vendedorId arriba), así que
        // no hace falta resolverlo en batch.
        Map<Long, String> vendedorUsernames = Map.of(vendedor.getId(), vendedor.getUsername());
        return productos.stream().map(p -> {
            double[] resena = statsResenas.getOrDefault(p.getId(), new double[]{ 0.0, 0 });
            long vendido    = statsVentas.getOrDefault(p.getId(), 0L);
            return ProductoResponseDTO.from(p, resena[0] > 0 ? resena[0] : null, (long) resena[1], vendido, vendedorUsernames);
        }).toList();
    }
}
