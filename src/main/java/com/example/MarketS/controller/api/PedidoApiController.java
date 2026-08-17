package com.example.MarketS.controller.api;

import com.example.MarketS.dto.CrearPedidoRequestDTO;
import com.example.MarketS.dto.PedidoResponseDTO;
import com.example.MarketS.exception.NoAutorizadoException;
import com.example.MarketS.exception.RecursoNoEncontradoException;
import com.example.MarketS.exception.SolicitudInvalidaException;
import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.Producto;
import com.example.MarketS.model.ResenaComprador;
import com.example.MarketS.model.ResolucionVentaExterna;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.repository.ResenaCompradorRepository;
import com.example.MarketS.repository.ResenaRepository;
import com.example.MarketS.repository.ResolucionVentaExternaRepository;
import com.example.MarketS.repository.UserRepository;
import com.example.MarketS.service.EmailService;
import com.example.MarketS.service.FrontendLinkBuilder;
import com.example.MarketS.service.MercadoPagoOAuthService;
import com.example.MarketS.service.MercadoPagoService;
import com.example.MarketS.service.NotificacionService;
import com.example.MarketS.service.PedidoService;
import com.example.MarketS.service.ProductoService;
import com.example.MarketS.security.UsuarioPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final MercadoPagoOAuthService mercadoPagoOAuthService;
    private final ProductoService productoService;
    private final NotificacionService notificacionService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final ResenaRepository resenaRepository;
    private final ResenaCompradorRepository resenaCompradorRepository;
    private final ResolucionVentaExternaRepository resolucionVentaExternaRepository;
    private final FrontendLinkBuilder frontendLinkBuilder;

    public PedidoApiController(PedidoService pedidoService,
                                PedidoRepository pedidoRepository,
                                MercadoPagoService mercadoPagoService,
                                MercadoPagoOAuthService mercadoPagoOAuthService,
                                ProductoService productoService,
                                NotificacionService notificacionService,
                                EmailService emailService,
                                UserRepository userRepository,
                                ResenaRepository resenaRepository,
                                ResenaCompradorRepository resenaCompradorRepository,
                                ResolucionVentaExternaRepository resolucionVentaExternaRepository,
                                FrontendLinkBuilder frontendLinkBuilder) {
        this.pedidoService = pedidoService;
        this.pedidoRepository = pedidoRepository;
        this.mercadoPagoService = mercadoPagoService;
        this.mercadoPagoOAuthService = mercadoPagoOAuthService;
        this.productoService = productoService;
        this.notificacionService = notificacionService;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.resenaRepository = resenaRepository;
        this.resenaCompradorRepository = resenaCompradorRepository;
        this.resolucionVentaExternaRepository = resolucionVentaExternaRepository;
        this.frontendLinkBuilder = frontendLinkBuilder;
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

        // Los administradores y moderadores no compran con su propia cuenta — evita el
        // conflicto de interés de que quien modera/administra el marketplace también sea
        // comprador en él. Validación en el backend además del bloqueo en el frontend
        // (CarritoPage), por si se llama a este endpoint directamente.
        String rol = usuario.getRol() != null ? usuario.getRol().getRolname() : "";
        if ("ROLE_ADMIN".equals(rol) || "ROLE_MODERADOR".equals(rol)) {
            throw new SolicitudInvalidaException("Los administradores y moderadores no pueden realizar compras con su cuenta.");
        }

        String metodoPago = request.getMetodoPago();
        boolean esAcuerdoExterno = metodoPago != null && METODOS_EXTERNOS.contains(metodoPago.toUpperCase());

        // El pago va directo a la cuenta de MercadoPago del vendedor (ver
        // MercadoPagoOAuthService) — si no la conectó, no hay a dónde mandar el dinero. Se valida
        // ANTES de crear el pedido: hacerlo después (como antes) ya disparaba el correo +
        // notificación de "pedido creado" para una compra que nunca pudo procesarse, y encima
        // dejaba un pedido RECHAZADO fantasma en el historial del comprador y del vendedor. El
        // carrito es de un solo vendedor (ver PedidoService.crearPedido), así que alcanza con
        // resolver el vendedor del primer ítem.
        String accessTokenVendedor = null;
        if (!esAcuerdoExterno) {
            User vendedorCarrito = productoService.obtenerProductoPorId(request.getItems().get(0).getProductoId()).getVendedor();
            accessTokenVendedor = vendedorCarrito != null
                    ? mercadoPagoOAuthService.obtenerAccessTokenVigente(vendedorCarrito).orElse(null)
                    : null;
            if (accessTokenVendedor == null) {
                throw new SolicitudInvalidaException(
                        "Este vendedor no tiene MercadoPago conectado. Elige un acuerdo externo (efectivo, transferencia, Nequi, otro) para completar la compra.");
            }
        }

        // Convertir DTO a Map<Integer, Integer> que espera PedidoService
        Map<Integer, Integer> carrito = request.getItems().stream()
                .collect(Collectors.toMap(
                        i -> i.getProductoId().intValue(),
                        CrearPedidoRequestDTO.ItemCarritoDTO::getCantidad
                ));

        Pedido pedido = pedidoService.crearPedido(usuario, carrito);

        if (esAcuerdoExterno) {
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
                successUrl, failureUrl, pendingUrl,
                accessTokenVendedor
        );

        if (!Boolean.TRUE.equals(mpResult.get("success"))) {
            String mpError = mpResult.containsKey("error") ? (String) mpResult.get("error") : "Error desconocido";
            System.err.println("ERROR MercadoPago al crear preferencia: " + mpError);
            System.err.println("Detalle respuesta MP: " + mpResult);

            // No se pudo iniciar el pago: el pedido no queda como PENDIENTE fantasma ni se avisa
            // "pedido creado" para una compra que nunca tuvo forma de pagarse — se marca
            // RECHAZADO y se reutiliza el mismo aviso que recibe un pago rechazado por MP,
            // que ya invita a intentar de nuevo desde el carrito.
            pedido.setEstado("RECHAZADO");
            pedidoRepository.save(pedido);
            notificarPagoRechazado(pedido);

            return ResponseEntity.status(500).body(Map.of(
                    "mensaje", "No se pudo iniciar el pago: " + mpError,
                    "pedidoId", pedido.getId()
            ));
        }

        String paymentUrl   = (String) mpResult.get("paymentUrl");
        String preferenceId = (String) mpResult.get("preferenceId");

        pedido.setEstado("PROCESANDO_PAGO");
        pedidoRepository.save(pedido);
        notificarPedidoConfirmado(pedido);

        Map<Long, String> vendedorUsernames = resolverVendedorUsernames(List.of(pedido));
        return ResponseEntity.ok(PedidoResponseDTO.from(pedido, paymentUrl, preferenceId, vendedorUsernames));
    }

    // Avisa "pedido creado" al comprador (correo + notificación in-app) y "nuevo pedido" a cada
    // vendedor afectado (notificación in-app + correo) — se llama recién cuando ya se sabe que
    // el pedido tiene una forma válida de pagarse (preferencia de MercadoPago creada, o acuerdo
    // externo registrado). Antes esto se disparaba al crear la fila del pedido, sin saber
    // todavía si el pago iba a poder iniciarse (ver PedidoService.crearPedido).
    private void notificarPedidoConfirmado(Pedido pedido) {
        User comprador = pedido.getUsuario();
        if (comprador.getEmail() != null && !comprador.getEmail().isBlank()) {
            emailService.enviarConfirmacionPedido(comprador.getEmail(), pedido);
        }
        notificacionService.crear(
                comprador,
                "Tu pedido #" + pedido.getId() + " fue creado. Total: $" + pedido.getTotal(),
                "PEDIDO_CREADO",
                pedido.getId()
        );

        String nombreComprador = comprador.getUsername();
        pedido.getDetalles().stream()
            .filter(d -> d.getProducto().getVendedor() != null)
            .collect(Collectors.groupingBy(d -> d.getProducto().getVendedor()))
            .forEach((vendedor, items) -> {
                String productos = items.stream()
                    .map(d -> d.getProducto().getNombre() + " (" + d.getCantidad() + " ud.)")
                    .collect(Collectors.joining(", "));
                notificacionService.crear(
                    vendedor,
                    nombreComprador + " ordenó: " + productos,
                    "NUEVO_PEDIDO",
                    pedido.getId()
                );
                if (vendedor.getEmail() != null && !vendedor.getEmail().isBlank()) {
                    emailService.enviarCorreo(
                            vendedor.getEmail(),
                            "Nuevo pedido #" + pedido.getId() + " - MarketS",
                            nombreComprador + " ordenó: " + productos
                                    + "\n\nVer en MarketS: " + frontendLinkBuilder.pedido(true, pedido.getId())
                    );
                }
            });
    }

    // La venta se acordó por fuera de la app (efectivo, transferencia, Nequi, etc.): no se genera
    // link de pago, el pedido queda a la espera de que el vendedor confirme si se concretó o no.
    private ResponseEntity<?> crearPedidoConAcuerdoExterno(Pedido pedido, String metodoPago) {
        pedido.setMetodoPago(metodoPago);
        pedido.setEstado("ACORDADO_EXTERNO");
        pedidoRepository.save(pedido);
        // Aviso genérico de "pedido creado" (comprador) + "nuevo pedido" (vendedores) — recién
        // acá, ya con el acuerdo externo registrado, no al armar el pedido.
        notificarPedidoConfirmado(pedido);

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
                                "Cuando se concrete o no la venta, márcalo desde tu panel de pedidos recibidos." +
                                "\n\nVer en MarketS: " + frontendLinkBuilder.pedido(true, pedido.getId())
                );
            }
        });

        // El comprador ya recibió el correo genérico de "pedido creado" (notificarPedidoConfirmado,
        // arriba), pero ese no menciona el método de pago ni que falta coordinar con el vendedor.
        // Este correo aparte lo mantiene al tanto de esa parte específica del flujo.
        User comprador = pedido.getUsuario();
        if (comprador.getEmail() != null && !comprador.getEmail().isBlank()) {
            emailService.enviarCorreo(
                    comprador.getEmail(),
                    "Tu pedido #" + pedido.getId() + " quedó acordado por fuera de la app - MarketS",
                    "Tu pedido #" + pedido.getId() + " se registró con pago acordado por fuera de la app (" + metodoPago + ").\n" +
                            "Coordiná la entrega y el pago con el vendedor por chat; una vez que se concrete, el vendedor lo marcará desde su panel." +
                            "\n\nVer en MarketS: " + frontendLinkBuilder.pedido(false, pedido.getId())
            );
        }

        Map<Long, String> vendedorUsernames = resolverVendedorUsernames(List.of(pedido));
        return ResponseEntity.ok(PedidoResponseDTO.from(pedido, null, null, vendedorUsernames));
    }

    // GET /api/pedidos/{id} — detalle de un pedido (solo el dueño)
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> obtener(@PathVariable Long id, Authentication auth) {
        User usuario = resolverUsuario(auth);
        Pedido pedido = pedidoRepository.findByIdWithDetalles(id)
                .filter(p -> p.getUsuario().getId().equals(usuario.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));
        return ResponseEntity.ok(PedidoResponseDTO.from(pedido, null, null, resolverVendedorUsernames(List.of(pedido))));
    }

    // GET /api/pedidos/mis-pedidos?page=0&size=30 — historial paginado del usuario autenticado.
    // Antes traía TODO el historial del comprador en una sola llamada (mismo problema que
    // ModeradorApiController.pedidos, ver comentario en
    // PedidoRepository.findAllWithUsuarioFiltrado). Se pagina primero solo sobre IDs (sin tocar
    // pedido.detalles, FetchType.EAGER) y el detalle completo se resuelve aparte solo para los
    // pedidos de esta página — ver PedidoRepository.findIdsByUsuario/findByIdsWithDetalles.
    @GetMapping("/mis-pedidos")
    public ResponseEntity<Map<String, Object>> misPedidos(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        User usuario = resolverUsuario(auth);

        Page<Long> pagina = pedidoRepository.findIdsByUsuario(usuario, PageRequest.of(page, size));
        List<Long> idsPagina = pagina.getContent();

        // findByIdsWithDetalles no garantiza el orden de la cláusula IN, así que se reordena
        // según idsPagina (ya ordenado por fecha DESC desde findIdsByUsuario).
        List<Pedido> pedidosConDetalles = idsPagina.isEmpty()
                ? List.of()
                : pedidoRepository.findByIdsWithDetalles(idsPagina);
        Map<Long, Pedido> pedidosPorId = pedidosConDetalles.stream()
                .collect(Collectors.toMap(Pedido::getId, p -> p));
        List<Pedido> pedidosEntidad = idsPagina.stream()
                .map(pedidosPorId::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<Long, String> vendedorUsernames = resolverVendedorUsernames(pedidosEntidad);

        List<Long> productoIds = pedidosEntidad.stream()
                .flatMap(p -> p.getDetalles().stream())
                .map(d -> d.getProducto().getId())
                .distinct()
                .toList();
        Set<Long> productosResenados = productoIds.isEmpty()
                ? Set.of()
                : new java.util.HashSet<>(resenaRepository.findProductoIdsResenadosByComprador(usuario.getId(), productoIds));

        List<PedidoResponseDTO> contenido = pedidosEntidad
                .stream()
                .map(p -> PedidoResponseDTO.from(p, null, null, vendedorUsernames, productosResenados))
                .toList();

        return ResponseEntity.ok(Map.of(
                "contenido", contenido,
                "paginaActual", pagina.getNumber(),
                "totalPaginas", pagina.getTotalPages(),
                "totalElementos", pagina.getTotalElements()
        ));
    }

    // PATCH /api/pedidos/{id}/confirmar-recepcion — el cliente confirma que recibió el producto
    @PatchMapping("/{id}/confirmar-recepcion")
    public ResponseEntity<?> confirmarRecepcion(@PathVariable Long id, Authentication auth) {
        User usuario = resolverUsuario(auth);
        Pedido pedido = pedidoRepository.findByIdWithDetalles(id).orElse(null);

        if (pedido == null) throw new RecursoNoEncontradoException("Pedido no encontrado");
        if (!pedido.getUsuario().getId().equals(usuario.getId()))
            throw new NoAutorizadoException("No autorizado");
        if (!"PAGADO".equals(pedido.getEstado()))
            throw new SolicitudInvalidaException("Solo se puede confirmar recepción de pedidos pagados");

        pedido.setEstado("RECIBIDO");
        pedidoRepository.save(pedido);

        // Avisar a cada vendedor involucrado: antes nadie se enteraba de que el comprador
        // confirmó la recepción.
        List<Long> vendedorIds = pedido.getDetalles().stream()
                .map(d -> d.getProducto().getVendedor())
                .filter(java.util.Objects::nonNull)
                .map(User::getId)
                .distinct()
                .toList();
        userRepository.findAllById(vendedorIds).forEach(vendedor -> {
            notificacionService.crear(vendedor,
                    usuario.getUsername() + " confirmó la recepción del pedido #" + id + ".",
                    "PEDIDO_RECIBIDO", id);
            if (vendedor.getEmail() != null && !vendedor.getEmail().isBlank()) {
                emailService.enviarCorreo(vendedor.getEmail(),
                        "El comprador confirmó la recepción del pedido #" + id + " - MarketS",
                        usuario.getUsername() + " confirmó que recibió su pedido #" + id + "."
                                + "\n\nVer en MarketS: " + frontendLinkBuilder.pedido(true, id));
            }
        });

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
        if (pedido == null) throw new RecursoNoEncontradoException("Pedido no encontrado");

        boolean esAdmin = "ROLE_ADMIN".equals(usuarioActual.getRol().getRolname());

        List<User> vendedoresDelPedido = pedido.getDetalles().stream()
                .map(d -> d.getProducto().getVendedor())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        List<Long> vendedorIds = vendedoresDelPedido.stream().map(User::getId).toList();

        boolean esVendedorDelPedido = vendedorIds.contains(usuarioActual.getId());
        if (!esAdmin && !esVendedorDelPedido) {
            throw new NoAutorizadoException("No autorizado");
        }

        if (!METODOS_EXTERNOS.contains(pedido.getMetodoPago())) {
            throw new SolicitudInvalidaException("Este pedido no es un acuerdo externo");
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
            throw new SolicitudInvalidaException("Este pedido ya fue resuelto");
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
                            avisarFalloDescuentoStock(d.getProducto(), pedido.getId(), e);
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
                emailService.enviarCorreo(comprador.getEmail(), "Actualización de tu pedido #" + id + " - MarketS",
                        mensaje + "\n\nVer en MarketS: " + frontendLinkBuilder.pedido(false, id));
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
        if (pedido == null) throw new RecursoNoEncontradoException("Pedido no encontrado");

        boolean esVendedorDelPedido = pedido.getDetalles().stream()
                .anyMatch(d -> d.getProducto().getVendedor() != null
                        && d.getProducto().getVendedor().getId().equals(vendedor.getId()));
        if (!esVendedorDelPedido) {
            throw new NoAutorizadoException("No autorizado");
        }

        // Para MercadoPago alcanza con el estado global (RECIBIDO no depende de otros
        // vendedores). Para acuerdos externos con varios vendedores, Pedido.estado puede seguir
        // en ACORDADO_EXTERNO mientras otro vendedor no resuelve la suya — lo que importa es si
        // ESTE vendedor ya completó su propia parte (ver ResolucionVentaExterna).
        boolean completoSuParte = resolucionVentaExternaRepository.findByPedidoIdAndVendedorId(id, vendedor.getId())
                .map(ResolucionVentaExterna::isCompletado)
                .orElse(false);
        if (!"RECIBIDO".equals(pedido.getEstado()) && !completoSuParte) {
            throw new SolicitudInvalidaException("Solo puedes calificar al comprador de un pedido completado");
        }

        if (resenaCompradorRepository.existsByPedidoIdAndVendedorId(id, vendedor.getId())) {
            throw new SolicitudInvalidaException("Ya calificaste al comprador de este pedido");
        }

        Object calObj = body.get("calificacion");
        if (calObj == null) {
            throw new SolicitudInvalidaException("La calificación es requerida");
        }
        int calificacion = Integer.parseInt(calObj.toString());
        if (calificacion < 1 || calificacion > 5) {
            throw new SolicitudInvalidaException("La calificación debe ser entre 1 y 5");
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

        // A pedido del usuario: calificar (comprador o vendedor) no genera notificación ni
        // correo, igual que las reseñas de producto.
        return ResponseEntity.ok(Map.of("mensaje", "Calificación registrada"));
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
                notificarPagoPendiente(p);
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
                notificarPagoRechazado(p);
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

        Map<String, Object> pago = mercadoPagoService.verificarPago(paymentId, resolverTokenVendedorPorPedido(pedidoId));

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

        Map<String, Object> pago = mercadoPagoService.verificarPago(paymentId, resolverTokenVendedorPorPedido(pedidoId));

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
                        // No interrumpe el flujo de pago (ya se cobró) pero avisa al vendedor
                        avisarFalloDescuentoStock(d.getProducto(), pedido.getId(), e);
                    }
                });
                notificarPagoAprobado(pedido);
            } else if ("pending".equals(mpStatus)) {
                pedido.setEstado("PENDIENTE_PAGO");
                pedidoRepository.save(pedido);
                notificarPagoPendiente(pedido);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Notificaciones de cambio de estado de pago (MercadoPago real o simulado) — antes nadie se
    // enteraba de que un pago se aprobó, quedó pendiente o fue rechazado; el comprador solo
    // recibía el correo de "pedido creado" al armar el carrito, antes de pagar.
    // -------------------------------------------------------------------------

    private void notificarPagoAprobado(Pedido pedido) {
        Long id = pedido.getId();
        userRepository.findById(pedido.getUsuario().getId()).ifPresent(comprador -> {
            notificacionService.crear(comprador, "Tu pago del pedido #" + id + " fue aprobado.", "PEDIDO_PAGADO", id);
            if (comprador.getEmail() != null && !comprador.getEmail().isBlank()) {
                emailService.enviarCorreo(comprador.getEmail(), "Pago aprobado - Pedido #" + id + " - MarketS",
                        "Tu pago del pedido #" + id + " fue aprobado. El vendedor ya puede alistar tu compra."
                                + "\n\nVer en MarketS: " + frontendLinkBuilder.pedido(false, id));
            }
        });

        List<Long> vendedorIds = pedido.getDetalles().stream()
                .map(d -> d.getProducto().getVendedor())
                .filter(java.util.Objects::nonNull)
                .map(User::getId)
                .distinct()
                .toList();
        userRepository.findAllById(vendedorIds).forEach(vendedor -> {
            notificacionService.crear(vendedor, "Se confirmó el pago de tu venta del pedido #" + id + ".", "VENTA_PAGO_CONFIRMADO", id);
            if (vendedor.getEmail() != null && !vendedor.getEmail().isBlank()) {
                emailService.enviarCorreo(vendedor.getEmail(), "Pago confirmado - Pedido #" + id + " - MarketS",
                        "Se confirmó el pago de tu venta del pedido #" + id + ". Ya podés alistar el envío."
                                + "\n\nVer en MarketS: " + frontendLinkBuilder.pedido(true, id));
            }
        });
    }

    private void notificarPagoPendiente(Pedido pedido) {
        Long id = pedido.getId();
        userRepository.findById(pedido.getUsuario().getId()).ifPresent(comprador -> {
            notificacionService.crear(comprador, "Tu pago del pedido #" + id + " está pendiente de confirmación.", "PEDIDO_PAGO_PENDIENTE", id);
            if (comprador.getEmail() != null && !comprador.getEmail().isBlank()) {
                emailService.enviarCorreo(comprador.getEmail(), "Pago pendiente - Pedido #" + id + " - MarketS",
                        "Tu pago del pedido #" + id + " está pendiente de confirmación por MercadoPago. Te avisaremos cuando se confirme."
                                + "\n\nVer en MarketS: " + frontendLinkBuilder.pedido(false, id));
            }
        });
    }

    // Se llama cuando reducirStock() falla al confirmar un pago/acuerdo ya cerrado (venta
    // duplicada por una carrera entre dos compradores comprando la última unidad al mismo
    // tiempo — pedidoService.crearPedido ya valida el stock al crear el pedido, así que esto
    // solo puede pasar en esa ventana de carrera). No se revierte el pago: el dinero ya cambió
    // de manos, así que en vez de solo loguearlo en consola (como antes) se avisa al vendedor
    // para que revise el inventario manualmente.
    private void avisarFalloDescuentoStock(Producto producto, Long pedidoId, Exception causa) {
        System.err.println("Error reduciendo stock producto " + producto.getId() + " (pedido " + pedidoId + "): " + causa.getMessage());

        Long vendedorId = producto.getVendedor() != null ? producto.getVendedor().getId() : null;
        if (vendedorId == null) return;

        userRepository.findById(vendedorId).ifPresent(vendedor -> {
            String mensaje = "No se pudo descontar el stock de \"" + producto.getNombre() + "\" para el pedido #" + pedidoId
                    + " (posible venta duplicada por stock insuficiente). Revisa el inventario manualmente.";
            notificacionService.crear(vendedor, mensaje, "STOCK_DESCUENTO_FALLIDO", pedidoId, producto.getId());
            if (vendedor.getEmail() != null && !vendedor.getEmail().isBlank()) {
                emailService.enviarCorreo(vendedor.getEmail(), "Revisa el stock de tu producto - MarketS", mensaje);
            }
        });
    }

    private void notificarPagoRechazado(Pedido pedido) {
        Long id = pedido.getId();
        userRepository.findById(pedido.getUsuario().getId()).ifPresent(comprador -> {
            notificacionService.crear(comprador, "Tu pago del pedido #" + id + " fue rechazado.", "PEDIDO_PAGO_RECHAZADO", id);
            if (comprador.getEmail() != null && !comprador.getEmail().isBlank()) {
                emailService.enviarCorreo(comprador.getEmail(), "Pago rechazado - Pedido #" + id + " - MarketS",
                        "Tu pago del pedido #" + id + " fue rechazado. Podés intentar de nuevo desde tu carrito."
                                + "\n\nVer en MarketS: " + frontendLinkBuilder.pedido(false, id));
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
        if (usuario == null) throw new RecursoNoEncontradoException("Usuario no encontrado: " + (auth != null ? auth.getName() : "anónimo"));
        return usuario;
    }

    // Un pedido es siempre de un solo vendedor (ver PedidoService.crearPedido); alcanza con el
    // vendedor del primer detalle que lo tenga.
    private User resolverVendedorDelPedido(Pedido pedido) {
        if (pedido.getDetalles() == null) return null;
        return pedido.getDetalles().stream()
                .map(d -> d.getProducto().getVendedor())
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    // Token del vendedor dueño del pedido para verificar un pago con la API de MP — null si el
    // pedido no tiene vendedor conectado (ej. órdenes creadas antes de conectar MercadoPago),
    // en cuyo caso MercadoPagoService.verificarPago cae al token de la plataforma.
    private String resolverTokenVendedorPorPedido(Long pedidoId) {
        if (pedidoId == null) return null;
        return pedidoRepository.findByIdWithDetalles(pedidoId)
                .map(this::resolverVendedorDelPedido)
                .filter(java.util.Objects::nonNull)
                .flatMap(mercadoPagoOAuthService::obtenerAccessTokenVigente)
                .orElse(null);
    }
}
