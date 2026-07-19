package com.example.MarketS.controller.api;

import com.example.MarketS.dto.CrearPedidoRequestDTO;
import com.example.MarketS.dto.PedidoResponseDTO;
import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.ResenaComprador;
import com.example.MarketS.model.ResolucionVentaExterna;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.repository.ResenaCompradorRepository;
import com.example.MarketS.repository.ResolucionVentaExternaRepository;
import com.example.MarketS.repository.UserRepository;
import com.example.MarketS.service.EmailService;
import com.example.MarketS.service.MercadoPagoService;
import com.example.MarketS.service.NotificacionService;
import com.example.MarketS.service.PedidoService;
import com.example.MarketS.service.ProductoService;
import com.example.MarketS.security.UsuarioPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
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
    private final NotificacionService notificacionService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final ResenaCompradorRepository resenaCompradorRepository;
    private final ResolucionVentaExternaRepository resolucionVentaExternaRepository;

    public PedidoApiController(PedidoService pedidoService,
                                PedidoRepository pedidoRepository,
                                MercadoPagoService mercadoPagoService,
                                ProductoService productoService,
                                NotificacionService notificacionService,
                                EmailService emailService,
                                UserRepository userRepository,
                                ResenaCompradorRepository resenaCompradorRepository,
                                ResolucionVentaExternaRepository resolucionVentaExternaRepository) {
        this.pedidoService = pedidoService;
        this.pedidoRepository = pedidoRepository;
        this.mercadoPagoService = mercadoPagoService;
        this.productoService = productoService;
        this.notificacionService = notificacionService;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.resenaCompradorRepository = resenaCompradorRepository;
        this.resolucionVentaExternaRepository = resolucionVentaExternaRepository;
    }

    // Resuelve en 1 sola query los usernames de todos los vendedores distintos presentes en
    // los pedidos, evitando cargar el User completo de cada vendedor (ver Producto.vendedor).
    private Map<Long, String> resolverVendedorUsernames(List<Pedido> pedidos) {
        List<Long> vendedorIds = pedidos.stream()
                .flatMap(p -> p.getDetalles().stream())
                .map(d -> d.getProducto().getVendedor() != null ? d.getProducto().getVendedor().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (vendedorIds.isEmpty()) return Map.of();
        Map<Long, String> resultado = new HashMap<>();
        userRepository.findIdAndUsernameByIdIn(vendedorIds).forEach(row ->
                resultado.put(((Number) row[0]).longValue(), (String) row[1]));
        return resultado;
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

        Map<Long, String> vendedorUsernames = resolverVendedorUsernames(List.of(pedido));
        return ResponseEntity.ok(PedidoResponseDTO.from(pedido, paymentUrl, preferenceId, vendedorUsernames));
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

        // producto.getVendedor() es LAZY: solo el id es seguro de leer aquí (la sesión que
        // cargó el producto ya se cerró). Se recargan los vendedores completos aparte para
        // poder leer su email.
        List<Long> vendedorIds = pedido.getDetalles().stream()
                .map(d -> d.getProducto().getVendedor())
                .filter(java.util.Objects::nonNull)
                .map(User::getId)
                .distinct()
                .toList();
        userRepository.findAllById(vendedorIds).forEach(vendedor -> {
            if (vendedor.getEmail() != null && !vendedor.getEmail().isBlank()) {
                emailService.enviarCorreo(
                        vendedor.getEmail(),
                        "Nuevo pedido acordado por fuera de la app - MarketS",
                        "Tienes un pedido #" + pedido.getId() + " con pago acordado por fuera de la app (" + metodoPago + ").\n" +
                                "Cuando se concrete o no la venta, márcalo desde tu panel de pedidos recibidos."
                );
            }
        });

        Map<Long, String> vendedorUsernames = resolverVendedorUsernames(List.of(pedido));
        return ResponseEntity.ok(PedidoResponseDTO.from(pedido, null, null, vendedorUsernames));
    }

    // GET /api/pedidos/{id} — detalle de un pedido (solo el dueño)
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> obtener(@PathVariable Long id, Authentication auth) {
        User usuario = resolverUsuario(auth);
        return pedidoRepository.findByIdWithDetalles(id)
                .filter(p -> p.getUsuario().getId().equals(usuario.getId()))
                .map(p -> PedidoResponseDTO.from(p, null, null, resolverVendedorUsernames(List.of(p))))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/pedidos/mis-pedidos — historial del usuario autenticado
    @GetMapping("/mis-pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> misPedidos(Authentication auth) {
        User usuario = resolverUsuario(auth);
        List<Pedido> pedidosEntidad = pedidoRepository.findByUsuarioWithDetails(usuario);
        Map<Long, String> vendedorUsernames = resolverVendedorUsernames(pedidosEntidad);
        List<PedidoResponseDTO> pedidos = pedidosEntidad
                .stream()
                .map(p -> PedidoResponseDTO.from(p, null, null, vendedorUsernames))
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

    // Un pedido puede tener productos de más de un vendedor (el carrito no se separa por
    // vendedor antes de pagar), así que cada vendedor confirma/rechaza SU parte de forma
    // independiente: la resolución se guarda por (pedido, vendedor) en ResolucionVentaExterna,
    // no en el único campo Pedido.estado — antes, el primer vendedor en actuar sobreescribía
    // ese campo compartido y dejaba a los demás bloqueados ("este pedido ya fue resuelto"),
    // sin poder resolver su parte ni descontar su propio stock.
    private ResponseEntity<?> resolverAcuerdoExterno(Long id, Authentication auth, boolean completado) {
        User usuarioActual = resolverUsuario(auth);
        Pedido pedido = pedidoRepository.findByIdWithDetalles(id).orElse(null);
        if (pedido == null) return ResponseEntity.notFound().build();

        boolean esAdmin = "ROLE_ADMIN".equals(usuarioActual.getRol().getRolname());

        List<User> vendedoresDelPedido = pedido.getDetalles().stream()
                .map(d -> d.getProducto().getVendedor())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        List<Long> vendedorIds = vendedoresDelPedido.stream().map(User::getId).toList();

        boolean esVendedorDelPedido = vendedorIds.contains(usuarioActual.getId());
        if (!esAdmin && !esVendedorDelPedido) {
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        }

        if (!METODOS_EXTERNOS.contains(pedido.getMetodoPago())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Este pedido no es un acuerdo externo"));
        }

        // El admin resuelve en nombre de TODOS los vendedores del pedido a la vez (no hay forma
        // de que elija "a nombre de cuál"); un vendedor normal solo resuelve su propia parte,
        // una sola vez.
        List<User> aResolver = esAdmin ? vendedoresDelPedido : List.of(usuarioActual);

        // Solo se resuelven (y descuentan stock de) los vendedores que TODAVÍA no habían
        // resuelto su parte — evita doble descuento si el admin vuelve a llamar el endpoint
        // sobre un pedido donde ya todos habían resuelto.
        List<User> nuevasResoluciones = aResolver.stream()
                .filter(v -> !resolucionVentaExternaRepository.existsByPedidoIdAndVendedorId(id, v.getId()))
                .toList();

        if (nuevasResoluciones.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Este pedido ya fue resuelto"));
        }

        nuevasResoluciones.forEach(v ->
                resolucionVentaExternaRepository.save(new ResolucionVentaExterna(pedido, v, completado)));

        if (completado) {
            List<Long> idsConStockADescontar = nuevasResoluciones.stream().map(User::getId).toList();
            pedido.getDetalles().stream()
                    .filter(d -> d.getProducto().getVendedor() != null
                            && idsConStockADescontar.contains(d.getProducto().getVendedor().getId()))
                    .forEach(d -> {
                        try {
                            productoService.reducirStock(d.getProducto().getId(), d.getCantidad());
                        } catch (Exception e) {
                            System.err.println("Error reduciendo stock: " + e.getMessage());
                        }
                    });
        }

        // El estado global del pedido solo se fija como definitivo cuando TODOS los vendedores
        // involucrados ya resolvieron su parte; mientras falte alguno, queda en ACORDADO_EXTERNO
        // para no bloquearlo. Si al menos uno completó, se considera concretado (aunque otro haya
        // rechazado la suya) porque el comprador sí recibió parte del pedido.
        List<ResolucionVentaExterna> resoluciones = resolucionVentaExternaRepository.findByPedidoId(id);
        Set<Long> yaResueltos = resoluciones.stream().map(r -> r.getVendedor().getId()).collect(Collectors.toSet());
        String nuevoEstado = pedido.getEstado();
        if (yaResueltos.containsAll(vendedorIds)) {
            boolean algunoCompletado = resoluciones.stream().anyMatch(ResolucionVentaExterna::isCompletado);
            nuevoEstado = algunoCompletado ? "COMPLETADO_EXTERNO" : "RECHAZADO_EXTERNO";
            pedido.setEstado(nuevoEstado);
            pedidoRepository.save(pedido);
        }

        User compradorRef = pedido.getUsuario(); // proxy LAZY: solo el id es seguro de leer aquí
        String mensaje = completado
                ? "El vendedor confirmó que tu pedido #" + id + " se realizó."
                : "El vendedor indicó que tu pedido #" + id + " no se llegó a realizar.";
        notificacionService.crear(compradorRef, mensaje, completado ? "PEDIDO_COMPLETADO_EXTERNO" : "PEDIDO_RECHAZADO_EXTERNO", id);
        userRepository.findById(compradorRef.getId()).ifPresent(comprador -> {
            if (comprador.getEmail() != null && !comprador.getEmail().isBlank()) {
                emailService.enviarCorreo(comprador.getEmail(), "Actualización de tu pedido #" + id + " - MarketS", mensaje);
            }
        });

        return ResponseEntity.ok(Map.of("mensaje", "Pedido actualizado", "estado", nuevoEstado));
    }

    // -------------------------------------------------------------------------
    // Reputación: el vendedor califica al comprador tras una transacción completada
    // -------------------------------------------------------------------------

    // PATCH /api/pedidos/{id}/calificar-comprador — solo cuando el pedido quedó RECIBIDO o
    // COMPLETADO_EXTERNO (ver ReputacionService). Una calificación por vendedor y por pedido.
    @PatchMapping("/{id}/calificar-comprador")
    public ResponseEntity<?> calificarComprador(@PathVariable Long id,
                                                 @RequestBody Map<String, Object> body,
                                                 Authentication auth) {
        User vendedor = resolverUsuario(auth);
        Pedido pedido = pedidoRepository.findByIdWithDetalles(id).orElse(null);
        if (pedido == null) return ResponseEntity.notFound().build();

        boolean esVendedorDelPedido = pedido.getDetalles().stream()
                .anyMatch(d -> d.getProducto().getVendedor() != null
                        && d.getProducto().getVendedor().getId().equals(vendedor.getId()));
        if (!esVendedorDelPedido) {
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        }

        // Para MercadoPago alcanza con el estado global (RECIBIDO no depende de otros
        // vendedores). Para acuerdos externos con varios vendedores, Pedido.estado puede seguir
        // en ACORDADO_EXTERNO mientras otro vendedor no resuelve la suya — lo que importa es si
        // ESTE vendedor ya completó su propia parte (ver ResolucionVentaExterna).
        boolean completoSuParte = resolucionVentaExternaRepository.findByPedidoIdAndVendedorId(id, vendedor.getId())
                .map(ResolucionVentaExterna::isCompletado)
                .orElse(false);
        if (!"RECIBIDO".equals(pedido.getEstado()) && !completoSuParte) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo puedes calificar al comprador de un pedido completado"));
        }

        if (resenaCompradorRepository.existsByPedidoIdAndVendedorId(id, vendedor.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ya calificaste al comprador de este pedido"));
        }

        Object calObj = body.get("calificacion");
        if (calObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "La calificación es requerida"));
        }
        int calificacion = Integer.parseInt(calObj.toString());
        if (calificacion < 1 || calificacion > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "La calificación debe ser entre 1 y 5"));
        }

        String comentario = body.get("comentario") instanceof String s ? s.trim() : null;
        if (comentario != null && comentario.isEmpty()) comentario = null;

        ResenaComprador resena = new ResenaComprador();
        resena.setPedido(pedido);
        resena.setVendedor(vendedor);
        resena.setComprador(pedido.getUsuario());
        resena.setCalificacion(calificacion);
        resena.setComentario(comentario);
        resena.setFecha(LocalDateTime.now());
        resenaCompradorRepository.save(resena);

        User compradorRef = pedido.getUsuario(); // proxy LAZY: solo el id es seguro de leer aquí
        notificacionService.crear(compradorRef,
                "El vendedor @" + vendedor.getUsername() + " te calificó con " + calificacion + " estrellas por el pedido #" + id + ".",
                "CALIFICACION_COMPRADOR", id);

        return ResponseEntity.ok(Map.of("mensaje", "Calificación registrada"));
    }

    // -------------------------------------------------------------------------
    // Simulación de pago exitoso — solo para desarrollo/sandbox
    // -------------------------------------------------------------------------

    @PostMapping("/{id}/simular-pago")
    public ResponseEntity<?> simularPago(@PathVariable Long id, Authentication auth) {
        User usuario = resolverUsuario(auth);

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
        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) throw new RuntimeException("Usuario no encontrado: " + (auth != null ? auth.getName() : "anónimo"));
        return usuario;
    }
}
