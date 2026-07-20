package com.example.MarketS.controller.api;

import com.example.MarketS.model.Encuentro;
import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.EncuentroRepository;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.security.UsuarioPrincipal;
import com.example.MarketS.service.EmailService;
import com.example.MarketS.service.FrontendLinkBuilder;
import com.example.MarketS.service.NotificacionService;
import com.example.MarketS.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pedidos/{pedidoId}/encuentro/{vendedorId}")
public class EncuentroApiController {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EncuentroRepository encuentroRepository;
    private final PedidoRepository pedidoRepository;
    private final UserService userService;
    private final NotificacionService notificacionService;
    private final EmailService emailService;
    private final FrontendLinkBuilder frontendLinkBuilder;

    public EncuentroApiController(EncuentroRepository encuentroRepository,
                                   PedidoRepository pedidoRepository,
                                   UserService userService,
                                   NotificacionService notificacionService,
                                   EmailService emailService,
                                   FrontendLinkBuilder frontendLinkBuilder) {
        this.encuentroRepository = encuentroRepository;
        this.pedidoRepository = pedidoRepository;
        this.userService = userService;
        this.notificacionService = notificacionService;
        this.emailService = emailService;
        this.frontendLinkBuilder = frontendLinkBuilder;
    }

    // GET /api/pedidos/{pedidoId}/encuentro/{vendedorId} — encuentro acordado (mapa vacío si no hay ninguno)
    @GetMapping
    public ResponseEntity<?> obtener(@PathVariable Long pedidoId, @PathVariable Long vendedorId, Authentication auth) {
        User usuario = resolverUsuario(auth);
        Pedido pedido = pedidoRepository.findByIdWithDetalles(pedidoId).orElse(null);
        if (pedido == null) return ResponseEntity.notFound().build();
        if (!autorizado(pedido, vendedorId, usuario)) {
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        }

        return encuentroRepository.findByPedidoIdAndVendedorId(pedidoId, vendedorId)
                .<ResponseEntity<?>>map(e -> ResponseEntity.ok(toMap(e)))
                .orElse(ResponseEntity.ok(Map.of()));
    }

    // PUT /api/pedidos/{pedidoId}/encuentro/{vendedorId} — propone o edita el encuentro (upsert)
    @PutMapping
    public ResponseEntity<?> guardar(@PathVariable Long pedidoId, @PathVariable Long vendedorId,
                                      @RequestBody Map<String, Object> body, Authentication auth) {
        User usuario = resolverUsuario(auth);
        Pedido pedido = pedidoRepository.findByIdWithDetalles(pedidoId).orElse(null);
        if (pedido == null) return ResponseEntity.notFound().build();
        if (!autorizado(pedido, vendedorId, usuario)) {
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        }

        String lugar = body.get("lugar") instanceof String s ? s.trim() : null;
        String fechaHoraStr = body.get("fechaHora") instanceof String s ? s.trim() : null;
        String nota = body.get("nota") instanceof String s ? s.trim() : null;

        if (lugar == null || lugar.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El lugar es obligatorio"));
        }
        if (fechaHoraStr == null || fechaHoraStr.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La fecha y hora son obligatorias"));
        }

        LocalDateTime fechaHora;
        try {
            fechaHora = LocalDateTime.parse(fechaHoraStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fecha y hora inválidas"));
        }
        if (fechaHora.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "La fecha del encuentro debe ser futura"));
        }

        User vendedor = vendedorId.equals(usuario.getId()) ? usuario : userService.buscarPorId(vendedorId).orElse(null);
        if (vendedor == null) return ResponseEntity.notFound().build();

        Optional<Encuentro> existente = encuentroRepository.findByPedidoIdAndVendedorId(pedidoId, vendedorId);
        boolean esNuevo = existente.isEmpty();
        Encuentro encuentro = existente.orElseGet(Encuentro::new);

        encuentro.setPedido(pedido);
        encuentro.setVendedor(vendedor);
        encuentro.setPropuestoPor(usuario);
        encuentro.setLugar(lugar);
        encuentro.setFechaHora(fechaHora);
        encuentro.setNota(nota != null && !nota.isEmpty() ? nota : null);
        if (esNuevo) encuentro.setFechaCreacion(LocalDateTime.now());
        encuentro.setFechaActualizacion(LocalDateTime.now());
        encuentroRepository.save(encuentro);

        String mensaje = esNuevo
                ? usuario.getUsername() + " propuso un encuentro para el pedido #" + pedidoId + ": " + lugar + " el " + fechaHora.format(FORMATO_FECHA)
                : usuario.getUsername() + " actualizó el encuentro del pedido #" + pedidoId + ": " + lugar + " el " + fechaHora.format(FORMATO_FECHA);
        notificarOtraParte(pedido, vendedorId, usuario, mensaje,
                esNuevo ? "ENCUENTRO_PROPUESTO" : "ENCUENTRO_ACTUALIZADO", pedidoId);

        return ResponseEntity.ok(toMap(encuentro));
    }

    // DELETE /api/pedidos/{pedidoId}/encuentro/{vendedorId} — cancela el encuentro acordado
    @DeleteMapping
    public ResponseEntity<?> cancelar(@PathVariable Long pedidoId, @PathVariable Long vendedorId, Authentication auth) {
        User usuario = resolverUsuario(auth);
        Pedido pedido = pedidoRepository.findByIdWithDetalles(pedidoId).orElse(null);
        if (pedido == null) return ResponseEntity.notFound().build();
        if (!autorizado(pedido, vendedorId, usuario)) {
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        }

        Encuentro encuentro = encuentroRepository.findByPedidoIdAndVendedorId(pedidoId, vendedorId).orElse(null);
        if (encuentro == null) return ResponseEntity.notFound().build();

        encuentroRepository.delete(encuentro);

        notificarOtraParte(pedido, vendedorId, usuario,
                usuario.getUsername() + " canceló el encuentro del pedido #" + pedidoId,
                "ENCUENTRO_CANCELADO", pedidoId);

        return ResponseEntity.ok(Map.of("cancelado", true));
    }

    // Resuelve al destinatario opuesto al que hizo la acción y le manda notificación + correo.
    // Siempre recarga el destinatario completo vía userService (en vez de navegar la entidad
    // Pedido/vendedor ya cargada) para no arriesgar LazyInitializationException fuera de la
    // transacción que originalmente cargó esas relaciones — mismo problema documentado en
    // ChatApiController/PedidoApiController para accesos a User vía relaciones LAZY.
    private void notificarOtraParte(Pedido pedido, Long vendedorId, User usuario, String mensaje, String tipo, Long pedidoId) {
        boolean esParaComprador = !usuario.getId().equals(pedido.getUsuario().getId());
        Long destinatarioId = esParaComprador ? pedido.getUsuario().getId() : vendedorId;
        User destinatario = userService.buscarPorId(destinatarioId).orElse(null);
        if (destinatario == null) return;

        notificacionService.crear(destinatario, mensaje, tipo, pedidoId, null, vendedorId);
        if (destinatario.getEmail() != null && !destinatario.getEmail().isBlank()) {
            String link = frontendLinkBuilder.pedidoConFoco(!esParaComprador, pedidoId, vendedorId, "encuentro");
            emailService.enviarCorreo(destinatario.getEmail(), "Encuentro del pedido #" + pedidoId + " - MarketS",
                    mensaje + "\n\nVer en MarketS: " + link);
        }
    }

    private boolean autorizado(Pedido pedido, Long vendedorId, User usuario) {
        boolean esAdmin = "ROLE_ADMIN".equals(usuario.getRol().getRolname());
        boolean esComprador = pedido.getUsuario().getId().equals(usuario.getId());
        boolean esVendedorDelPedido = usuario.getId().equals(vendedorId)
                && pedido.getDetalles().stream()
                        .anyMatch(d -> d.getProducto().getVendedor() != null
                                && d.getProducto().getVendedor().getId().equals(vendedorId));
        return esAdmin || esComprador || esVendedorDelPedido;
    }

    private User resolverUsuario(Authentication auth) {
        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) throw new RuntimeException("Usuario no encontrado: " + (auth != null ? auth.getName() : "anónimo"));
        return usuario;
    }

    private Map<String, Object> toMap(Encuentro e) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", e.getId());
        map.put("lugar", e.getLugar());
        map.put("fechaHora", e.getFechaHora().toString());
        map.put("nota", e.getNota());
        map.put("propuestoPorId", e.getPropuestoPor().getId());
        map.put("propuestoPorUsername", e.getPropuestoPor().getUsername());
        return map;
    }
}
