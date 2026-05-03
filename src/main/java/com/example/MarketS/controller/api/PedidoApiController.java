package com.example.MarketS.controller.api;

import com.example.MarketS.dto.CrearPedidoRequestDTO;
import com.example.MarketS.dto.PedidoResponseDTO;
import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.service.MercadoPagoService;
import com.example.MarketS.service.PedidoService;
import com.example.MarketS.service.ProductoService;
import com.example.MarketS.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoApiController {

    @Value("${app.url:http://localhost:8081}")
    private String appUrl;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepository;
    private final MercadoPagoService mercadoPagoService;
    private final ProductoService productoService;
    private final UserService userService;

    public PedidoApiController(PedidoService pedidoService,
                                PedidoRepository pedidoRepository,
                                MercadoPagoService mercadoPagoService,
                                ProductoService productoService,
                                UserService userService) {
        this.pedidoService = pedidoService;
        this.pedidoRepository = pedidoRepository;
        this.mercadoPagoService = mercadoPagoService;
        this.productoService = productoService;
        this.userService = userService;
    }

    // POST /api/pedidos — crea pedido y devuelve URL de pago de MercadoPago
    @PostMapping
    public ResponseEntity<?> crearPedido(@Valid @RequestBody CrearPedidoRequestDTO request,
                                          Authentication auth) {
        User usuario = resolverUsuario(auth);

        // Convertir DTO a Map<Integer, Integer> que espera PedidoService
        Map<Integer, Integer> carrito = request.getItems().stream()
                .collect(Collectors.toMap(
                        i -> i.getProductoId().intValue(),
                        CrearPedidoRequestDTO.ItemCarritoDTO::getCantidad
                ));

        Pedido pedido;
        try {
            pedido = pedidoService.crearPedido(usuario, carrito);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        // Generar preferencia de MercadoPago con back_urls al frontend React
        String successUrl  = appUrl + "/api/pedidos/mp/exito?pedidoId=" + pedido.getId();
        String failureUrl  = appUrl + "/api/pedidos/mp/fallo?pedidoId=" + pedido.getId();
        String pendingUrl  = appUrl + "/api/pedidos/mp/pendiente?pedidoId=" + pedido.getId();

        Map<String, Object> mpResult = mercadoPagoService.crearPreferencia(
                pedido.getId(),
                pedido.getTotal(),
                "Compra MarketS - Pedido #" + pedido.getId(),
                usuario.getEmail(),
                successUrl, failureUrl, pendingUrl
        );

        String paymentUrl   = Boolean.TRUE.equals(mpResult.get("success")) ? (String) mpResult.get("paymentUrl") : null;
        String preferenceId = Boolean.TRUE.equals(mpResult.get("success")) ? (String) mpResult.get("preferenceId") : null;

        if (paymentUrl != null) {
            pedido.setEstado("PROCESANDO_PAGO");
            pedidoRepository.save(pedido);
        }

        return ResponseEntity.ok(PedidoResponseDTO.from(pedido, paymentUrl, preferenceId));
    }

    // GET /api/pedidos/{id} — detalle de un pedido (solo el dueño)
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> obtener(@PathVariable Long id, Authentication auth) {
        User usuario = resolverUsuario(auth);
        return pedidoRepository.findByIdWithDetalles(id)
                .filter(p -> p.getUsuario().getId().equals(usuario.getId()))
                .map(PedidoResponseDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/pedidos/mis-pedidos — historial del usuario autenticado
    @GetMapping("/mis-pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> misPedidos(Authentication auth) {
        User usuario = resolverUsuario(auth);
        List<PedidoResponseDTO> pedidos = pedidoRepository.findByUsuario(usuario)
                .stream()
                .map(PedidoResponseDTO::from)
                .toList();
        return ResponseEntity.ok(pedidos);
    }

    // -------------------------------------------------------------------------
    // Callbacks de MercadoPago — públicos, el browser del usuario aterriza aquí
    // y el backend procesa el estado antes de redirigir al frontend React.
    // -------------------------------------------------------------------------

    @GetMapping("/mp/exito")
    public ResponseEntity<Void> mpExito(@RequestParam Map<String, String> params,
                                         @RequestParam(required = false) Long pedidoId) {
        Long id = resolverPedidoId(params, pedidoId);
        if (id != null) {
            actualizarEstado(id, params, "approved");
        }
        return redirigirFrontend(id, "approved");
    }

    @GetMapping("/mp/pendiente")
    public ResponseEntity<Void> mpPendiente(@RequestParam Map<String, String> params,
                                             @RequestParam(required = false) Long pedidoId) {
        Long id = resolverPedidoId(params, pedidoId);
        if (id != null) {
            pedidoRepository.findById(id).ifPresent(p -> {
                p.setEstado("PENDIENTE_PAGO");
                pedidoRepository.save(p);
            });
        }
        return redirigirFrontend(id, "pending");
    }

    @GetMapping("/mp/fallo")
    public ResponseEntity<Void> mpFallo(@RequestParam Map<String, String> params,
                                         @RequestParam(required = false) Long pedidoId) {
        Long id = resolverPedidoId(params, pedidoId);
        if (id != null) {
            pedidoRepository.findById(id).ifPresent(p -> {
                p.setEstado("RECHAZADO");
                pedidoRepository.save(p);
            });
        }
        return redirigirFrontend(id, "rejected");
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

    private void actualizarEstado(Long pedidoId, Map<String, String> params, String mpStatus) {
        pedidoRepository.findByIdWithDetalles(pedidoId).ifPresent(pedido -> {
            String collectionStatus = params.getOrDefault("collection_status", "");
            boolean aprobado = "approved".equals(mpStatus) || "approved".equals(collectionStatus);

            if (aprobado) {
                pedido.setEstado("PAGADO");
                pedidoRepository.save(pedido);
                // Reducir stock por cada detalle
                pedido.getDetalles().forEach(d -> {
                    try {
                        productoService.reducirStock(d.getProducto().getId(), d.getCantidad());
                    } catch (Exception e) {
                        // Log pero no interrumpir el flujo de pago
                        System.err.println("Error reduciendo stock producto " + d.getProducto().getId() + ": " + e.getMessage());
                    }
                });
            } else if ("pending".equals(mpStatus)) {
                pedido.setEstado("PENDIENTE_PAGO");
                pedidoRepository.save(pedido);
            }
        });
    }

    private ResponseEntity<Void> redirigirFrontend(Long pedidoId, String status) {
        String path = pedidoId != null
                ? "/pedidos/" + pedidoId + "/confirmacion?status=" + status
                : "/pedidos?status=" + status;
        return ResponseEntity.status(302)
                .location(URI.create(frontendUrl + path))
                .build();
    }

    private Long resolverPedidoId(Map<String, String> params, Long pedidoIdParam) {
        if (pedidoIdParam != null) return pedidoIdParam;
        String ref = params.get("external_reference");
        if (ref != null) {
            try { return Long.parseLong(ref); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private User resolverUsuario(Authentication auth) {
        return userService.buscarPorEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + auth.getName()));
    }
}
