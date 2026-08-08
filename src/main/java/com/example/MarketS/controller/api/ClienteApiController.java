package com.example.MarketS.controller.api;

import com.example.MarketS.dto.PedidoResponseDTO;
import com.example.MarketS.dto.PerfilResponseDTO;
import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.Rol;
import com.example.MarketS.model.User;
import com.example.MarketS.model.Vendedor;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.repository.RolRepository;
import com.example.MarketS.repository.UserRepository;
import com.example.MarketS.security.AuthCookieService;
import com.example.MarketS.security.JwtTokenProvider;
import com.example.MarketS.service.EmailService;
import com.example.MarketS.service.NotificacionService;
import com.example.MarketS.service.ReputacionService;
import com.example.MarketS.security.UsuarioPrincipal;
import com.example.MarketS.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cliente")
@PreAuthorize("hasAnyAuthority('ROLE_CLIENTE', 'ROLE_VENDEDOR', 'ROLE_ADMIN')")
public class ClienteApiController {

    // Misma vida útil que el JWT (jwt.expiration) — así la cookie nunca vence antes que el token.
    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private Duration cookieMaxAge() {
        return Duration.ofMillis(jwtExpirationMs);
    }

    private final PedidoRepository pedidoRepository;
    private final UserRepository userRepository;
    private final ReputacionService reputacionService;
    private final RolRepository rolRepository;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final AuthCookieService cookieService;
    private final EmailService emailService;
    private final NotificacionService notificacionService;

    public ClienteApiController(PedidoRepository pedidoRepository, UserRepository userRepository,
                                 ReputacionService reputacionService, RolRepository rolRepository,
                                 UserService userService, JwtTokenProvider tokenProvider,
                                 AuthCookieService cookieService, EmailService emailService,
                                 NotificacionService notificacionService) {
        this.pedidoRepository = pedidoRepository;
        this.userRepository = userRepository;
        this.reputacionService = reputacionService;
        this.rolRepository = rolRepository;
        this.userService = userService;
        this.tokenProvider = tokenProvider;
        this.cookieService = cookieService;
        this.emailService = emailService;
        this.notificacionService = notificacionService;
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

    // POST /api/cliente/convertir-vendedor — self-service "Vender" (al estilo MercadoLibre):
    // un CLIENTE se convierte en VENDEDOR sin pasar por el admin. Reemite el JWT con el rol
    // nuevo para que el frontend actualice la sesión sin forzar un logout/login.
    @PostMapping("/convertir-vendedor")
    @PreAuthorize("hasAuthority('ROLE_CLIENTE')")
    public ResponseEntity<?> convertirVendedor(@RequestBody Map<String, String> body,
                                                Authentication auth,
                                                HttpServletResponse response) {
        String city = body.get("city");
        if (city == null || city.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La ciudad es obligatoria para vender"));
        }

        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) throw new RuntimeException("Usuario no encontrado");

        Rol rolVendedor = rolRepository.findByRolname("ROLE_VENDEDOR")
                .orElseThrow(() -> new IllegalStateException("Rol VENDEDOR no configurado"));

        Vendedor vendedor = new Vendedor();
        vendedor.setCity(city.trim());
        vendedor.setUser(usuario);
        usuario.setVendedor(vendedor);
        usuario.setRol(rolVendedor);
        userService.guardar(usuario);

        emailService.enviarGuiaMercadoPago(usuario.getEmail(), usuario.getUsername());
        notificacionService.crear(usuario,
                "Ya sos vendedor en MarketS. Configurá tu cuenta de MercadoPago para poder cobrar tus ventas.",
                "VENDEDOR_CONFIGURAR_MP", null);

        Authentication authStub = new UsernamePasswordAuthenticationToken(
                usuario.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_VENDEDOR")));
        String token = tokenProvider.generateToken(authStub);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildCookie(token, cookieMaxAge()).toString());

        return ResponseEntity.ok(Map.of("email", usuario.getEmail(), "role", "VENDEDOR", "token", token));
    }
}
