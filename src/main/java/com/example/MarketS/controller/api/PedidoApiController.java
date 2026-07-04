package com.example.MarketS.controller.api;

import com.example.MarketS.dto.CrearPedidoRequestDTO;
import com.example.MarketS.dto.PedidoResponseDTO;
import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.service.EmailService;
import com.example.MarketS.service.MercadoPagoService;
import com.example.MarketS.service.NotificacionService;
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
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoApiController {

    @Value("${app.url:http://localhost:8081}")
    private String appUrl;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private static final Set<String> METODOS_EXTERNOS = Set.of("EFECTIVO", "TRANSFERENCIA", "NEQUI", "OTRO");

    private final PedidoService pedidoService;
    private final PedidoRepository pedidoRepository;
    private final MercadoPagoService mercadoPagoService;
    private final ProductoService productoService;
    private final UserService userService;
    private final NotificacionService notificacionService;
    private final EmailService emailService;

    public PedidoApiController(PedidoService pedidoService,
                                PedidoRepository pedidoRepository,
                                MercadoPagoService mercadoPagoService,
                                ProductoService productoService,
                                UserService userService,
                                NotificacionService notificacionService,
                                EmailService emailService) {
        this.pedidoService = pedidoService;
        this.pedidoRepository = pedidoRepository;
        this.mercadoPagoService = mercadoPagoService;
        this.productoService = productoService;
        this.userService = userService;
        this.notificacionService = notificacionService;
        this.emailService = emailService;
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

        String metodoPago = request.getMetodoPago();
        if (metodoPago != null && METODOS_EXTERNOS.contains(metodoPago.toUpperCase())) {
            return crearPedidoConAcuerdoExterno(pedido, metodoPago.toUpperCase());
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

        if (!Boolean.TRUE.equals(mpResult.get("success"))) {
            String mpError = mpResult.containsKey("error") ? (String) mpResult.get("error") : "Error desconocido";
            System.err.println("ERROR MercadoPago al crear preferencia: " + mpError);
            System.err.println("Detalle respuesta MP: " + mpResult);
            return ResponseEntity.status(500).body(Map.of(
                    "mensaje", "No se pudo iniciar el pago: " + mpError,
                    "pedidoId", pedido.getId()
            ));
        }

        String paymentUrl   = (String) mpResult.get("paymentUrl");
        String preferenceId = (String) mpResult.get("preferenceId");

        pedido.setEstado("PROCESANDO_PAGO");
        pedidoRepository.save(pedido);

        return ResponseEntity.ok(PedidoResponseDTO.from(pedido, paymentUrl, preferenceId));
    }

    // La venta se acordó por fuera de la app (efectivo, transferencia, Nequi, etc.): no se genera
    // link de pago, el pedido queda a la espera de que el vendedor confirme si se concretó o no.
    // pedidoService.crearPedido ya notificó (in-app) a cada vendedor con "NUEVO_PEDIDO"; aquí solo
    // se avisa por correo, ya que ese flujo genérico no envía email al vendedor y este caso requiere
    // una acción suya (confirmar o rechazar la venta).
    private ResponseEntity<?> crearPedidoConAcuerdoExterno(Pedido pedido, String metodoPago) {
        pedido.setMetodoPago(metodoPago);
        pedido.setEstado("ACORDADO_EXTERNO");
        pedidoRepository.save(pedido);

        pedido.getDetalles().stream()
                .map(d -> d.getProducto().getVendedor())
                .distinct()
                .filter(vendedor -> vendedor.getEmail() != null && !vendedor.getEmail().isBlank())
                .forEach(vendedor -> emailService.enviarCorreo(
                        vendedor.getEmail(),
                        "Nuevo pedido acordado por fuera de la app - MarketS",
                        "Tienes un pedido #" + pedido.getId() + " con pago acordado por fuera de la app (" + metodoPago + ").\n" +
                                "Cuando se concrete o no la venta, márcalo desde tu panel de pedidos recibidos."
                ));

        return ResponseEntity.ok(PedidoResponseDTO.from(pedido));
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
        List<PedidoResponseDTO> pedidos = pedidoRepository.findByUsuarioWithDetails(usuario)
                .stream()
                .map(PedidoResponseDTO::from)
                .toList();
        return ResponseEntity.ok(pedidos);
    }

    // PATCH /api/pedidos/{id}/confirmar-recepcion — el cliente confirma que recibió el producto
    @PatchMapping("/{id}/confirmar-recepcion")
    public ResponseEntity<?> confirmarRecepcion(@PathVariable Long id, Authentication auth) {
        User usuario = resolverUsuario(auth);
        Pedido pedido = pedidoRepository.findById(id).orElse(null);

        if (pedido == null) return ResponseEntity.notFound().build();
        if (!pedido.getUsuario().getId().equals(usuario.getId()))
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        if (!"PAGADO".equals(pedido.getEstado()))
            return ResponseEntity.badRequest().body(Map.of("error", "Solo se puede confirmar recepción de pedidos pagados"));

        pedido.setEstado("RECIBIDO");
        pedidoRepository.save(pedido);
        return ResponseEntity.ok(Map.of("mensaje", "Recepción confirmada", "estado", "RECIBIDO"));
    }

    // -------------------------------------------------------------------------
    // Confirmación manual de ventas acordadas por fuera de la app (efectivo, transferencia, etc.)
    // -------------------------------------------------------------------------

    // PATCH /api/pedidos/{id}/marcar-completado — el vendedor confirma que la venta externa se concretó
    @PatchMapping("/{id}/marcar-completado")
    public ResponseEntity<?> marcarCompletado(@PathVariable Long id, Authentication auth) {
        return resolverAcuerdoExterno(id, auth, true);
    }

    // PATCH /api/pedidos/{id}/marcar-rechazado — el vendedor indica que la venta externa no se concretó
    @PatchMapping("/{id}/marcar-rechazado")
    public ResponseEntity<?> marcarRechazado(@PathVariable Long id, Authentication auth) {
        return resolverAcuerdoExterno(id, auth, false);
    }

    private ResponseEntity<?> resolverAcuerdoExterno(Long id, Authentication auth, boolean completado) {
        User vendedor = resolverUsuario(auth);
        Pedido pedido = pedidoRepository.findByIdWithDetalles(id).orElse(null);
        if (pedido == null) return ResponseEntity.notFound().build();

        boolean esAdmin = "ROLE_ADMIN".equals(vendedor.getRol().getRolname());
        boolean esVendedorDelPedido = pedido.getDetalles().stream()
                .anyMatch(d -> d.getProducto().getVendedor() != null
                        && d.getProducto().getVendedor().getId().equals(vendedor.getId()));
        if (!esAdmin && !esVendedorDelPedido) {
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        }

        if (!METODOS_EXTERNOS.contains(pedido.getMetodoPago())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Este pedido no es un acuerdo externo"));
        }
        if (!"ACORDADO_EXTERNO".equals(pedido.getEstado())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Este pedido ya fue resuelto"));
        }

        String nuevoEstado = completado ? "COMPLETADO_EXTERNO" : "RECHAZADO_EXTERNO";
        pedido.setEstado(nuevoEstado);
        pedidoRepository.save(pedido);

        if (completado) {
            pedido.getDetalles().stream()
                    .filter(d -> d.getProducto().getVendedor().getId().equals(vendedor.getId()))
                    .forEach(d -> {
                        try {
                            productoService.reducirStock(d.getProducto().getId(), d.getCantidad());
                        } catch (Exception e) {
                            System.err.println("Error reduciendo stock: " + e.getMessage());
                        }
                    });
        }

        User comprador = pedido.getUsuario();
        String mensaje = completado
                ? "El vendedor confirmó que tu pedido #" + id + " se realizó."
                : "El vendedor indicó que tu pedido #" + id + " no se llegó a realizar.";
        notificacionService.crear(comprador, mensaje, completado ? "PEDIDO_COMPLETADO_EXTERNO" : "PEDIDO_RECHAZADO_EXTERNO", id);
        if (comprador.getEmail() != null && !comprador.getEmail().isBlank()) {
            emailService.enviarCorreo(comprador.getEmail(), "Actualización de tu pedido #" + id + " - MarketS", mensaje);
        }

        return ResponseEntity.ok(Map.of("mensaje", "Pedido actualizado", "estado", nuevoEstado));
    }

    // -------------------------------------------------------------------------
    // Simulación de pago exitoso — solo para desarrollo/sandbox
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/simular-pago")
    public ResponseEntity<?> simularPago(@PathVariable Long id, Authentication auth) {
        User usuario = userService.buscarPorEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Pedido pedido = pedidoRepository.findByIdWithDetalles(id).orElse(null);
        if (pedido == null) {
            return ResponseEntity.notFound().build();
        }
        if (!pedido.getUsuario().getId().equals(usuario.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        }
        if ("PAGADO".equals(pedido.getEstado())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El pedido ya está pagado"));
        }

        pedido.setEstado("PAGADO");
        pedidoRepository.save(pedido);

        pedido.getDetalles().forEach(d -> {
            try {
                productoService.reducirStock(d.getProducto().getId(), d.getCantidad());
            } catch (Exception e) {
                System.err.println("Error reduciendo stock: " + e.getMessage());
            }
        });

        return ResponseEntity.ok(Map.of("mensaje", "Pago simulado correctamente", "estado", "PAGADO"));
    }

    // -------------------------------------------------------------------------
    // Callbacks de MercadoPago — públicos, el browser del usuario aterriza aquí
    // y el backend procesa el estado antes de redirigir al frontend React.
    // -------------------------------------------------------------------------

    @GetMapping("/mp/exito")
    public ResponseEntity<Void> mpExito(@RequestParam Map<String, String> params,
                                         @RequestParam(required = false) Long pedidoId) {
        Long id = resolverPedidoId(params, pedidoId);
        if (id != null && verificarPagoAprobado(params, id)) {
            actualizarEstado(id, params, "approved");
        }
        return redirigirFrontend(id, "approved");
    }

    @GetMapping("/mp/pendiente")
    public ResponseEntity<Void> mpPendiente(@RequestParam Map<String, String> params,
                                             @RequestParam(required = false) Long pedidoId) {
        Long id = resolverPedidoId(params, pedidoId);
        if (id != null && verificarPagoConMP(params, id, "pending")) {
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
        if (id != null && verificarPagoConMP(params, id, "rejected")) {
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

    // Verifica con la API de MP que el payment_id es real, está aprobado
    // y su external_reference coincide con el pedidoId local.
    private boolean verificarPagoAprobado(Map<String, String> params, Long pedidoId) {
        String paymentId = params.getOrDefault("payment_id",
                params.getOrDefault("collection_id", null));

        if (paymentId == null || paymentId.isBlank()) {
            return false;
        }

        Map<String, Object> pago = mercadoPagoService.verificarPago(paymentId);

        if (!Boolean.TRUE.equals(pago.get("valido"))) {
            return false;
        }

        boolean statusAprobado = "approved".equals(pago.get("status"));
        boolean referenciaCoincide = pedidoId.toString().equals(pago.get("externalReference"));

        return statusAprobado && referenciaCoincide;
    }

    // Para pendiente y fallo: verifica que el payment_id pertenece al pedido,
    // sin exigir status aprobado.
    private boolean verificarPagoConMP(Map<String, String> params, Long pedidoId, String statusEsperado) {
        String paymentId = params.getOrDefault("payment_id",
                params.getOrDefault("collection_id", null));

        if (paymentId == null || paymentId.isBlank()) {
            return false;
        }

        Map<String, Object> pago = mercadoPagoService.verificarPago(paymentId);

        if (!Boolean.TRUE.equals(pago.get("valido"))) {
            return false;
        }

        return pedidoId.toString().equals(pago.get("externalReference"));
    }

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
        String frontendStatus = switch (status) {
            case "approved" -> "exito";
            case "pending"  -> "pendiente";
            default         -> "fallo";
        };
        String path = pedidoId != null
                ? "/pedidos/confirmacion?status=" + frontendStatus + "&pedido_id=" + pedidoId
                : "/pedidos/confirmacion?status=" + frontendStatus;
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
