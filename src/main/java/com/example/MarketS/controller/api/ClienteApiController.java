package com.example.MarketS.controller.api;

import com.example.MarketS.dto.PedidoResponseDTO;
import com.example.MarketS.dto.PerfilResponseDTO;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cliente")
@PreAuthorize("hasAnyAuthority('ROLE_CLIENTE', 'ROLE_VENDEDOR', 'ROLE_ADMIN')")
public class ClienteApiController {

    private final UserService userService;
    private final PedidoRepository pedidoRepository;

    public ClienteApiController(UserService userService, PedidoRepository pedidoRepository) {
        this.userService = userService;
        this.pedidoRepository = pedidoRepository;
    }

    // GET /api/cliente/dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(Authentication auth) {
        User usuario = userService.buscarPorEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<PedidoResponseDTO> pedidos = pedidoRepository.findByUsuario(usuario)
                .stream()
                .map(PedidoResponseDTO::from)
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
                "gastoTotal", gastoTotal
        ));
    }
}
