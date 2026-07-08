package com.example.MarketS.controller.api;

import com.example.MarketS.dto.PedidoResponseDTO;
import com.example.MarketS.dto.PerfilResponseDTO;
import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.repository.UserRepository;
import com.example.MarketS.service.ReputacionService;
import com.example.MarketS.security.UsuarioPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cliente")
@PreAuthorize("hasAnyAuthority('ROLE_CLIENTE', 'ROLE_VENDEDOR', 'ROLE_ADMIN')")
public class ClienteApiController {

    private final PedidoRepository pedidoRepository;
    private final UserRepository userRepository;
    private final ReputacionService reputacionService;

    public ClienteApiController(PedidoRepository pedidoRepository, UserRepository userRepository,
                                 ReputacionService reputacionService) {
        this.pedidoRepository = pedidoRepository;
        this.userRepository = userRepository;
        this.reputacionService = reputacionService;
    }

    // GET /api/cliente/dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(Authentication auth) {
        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) throw new RuntimeException("Usuario no encontrado");

        List<Pedido> pedidosEntidad = pedidoRepository.findByUsuarioWithDetails(usuario);

        // Resuelve en 1 sola query los usernames de vendedores distintos (evita cargar el
        // User completo de cada uno, ver Producto.vendedor).
        List<Long> vendedorIds = pedidosEntidad.stream()
                .flatMap(p -> p.getDetalles().stream())
                .map(d -> d.getProducto().getVendedor() != null ? d.getProducto().getVendedor().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> vendedorUsernames = new HashMap<>();
        if (!vendedorIds.isEmpty()) {
            userRepository.findIdAndUsernameByIdIn(vendedorIds).forEach(row ->
                    vendedorUsernames.put(((Number) row[0]).longValue(), (String) row[1]));
        }

        List<PedidoResponseDTO> pedidos = pedidosEntidad
                .stream()
                .map(p -> PedidoResponseDTO.from(p, null, null, vendedorUsernames))
                .toList();

        double gastoTotal = pedidos.stream()
                .filter(p -> "PAGADO".equals(p.getEstado()))
                .mapToDouble(PedidoResponseDTO::getTotal)
                .sum();

        long pedidosPagados = pedidos.stream()
                .filter(p -> "PAGADO".equals(p.getEstado()))
                .count();

        List<PedidoResponseDTO> recientes = pedidos.stream()
                .sorted((a, b) -> b.getFecha().compareTo(a.getFecha()))
                .limit(5)
                .toList();

        return ResponseEntity.ok(Map.of(
                "perfil", PerfilResponseDTO.from(usuario),
                "pedidosRecientes", recientes,
                "totalPedidos", pedidos.size(),
                "pedidosPagados", pedidosPagados,
                "gastoTotal", gastoTotal,
                "reputacion", reputacionService.calcularComprador(usuario.getId())
        ));
    }
}
