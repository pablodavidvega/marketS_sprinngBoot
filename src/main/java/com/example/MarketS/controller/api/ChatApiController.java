package com.example.MarketS.controller.api;

import com.example.MarketS.model.ChatMensaje;
import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.ChatMensajeRepository;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.service.EmailService;
import com.example.MarketS.service.NotificacionService;
import com.example.MarketS.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos/{pedidoId}/chat/{vendedorId}")
public class ChatApiController {

    private final ChatMensajeRepository chatMensajeRepository;
    private final PedidoRepository pedidoRepository;
    private final UserService userService;
    private final NotificacionService notificacionService;
    private final EmailService emailService;

    public ChatApiController(ChatMensajeRepository chatMensajeRepository,
                              PedidoRepository pedidoRepository,
                              UserService userService,
                              NotificacionService notificacionService,
                              EmailService emailService) {
        this.chatMensajeRepository = chatMensajeRepository;
        this.pedidoRepository = pedidoRepository;
        this.userService = userService;
        this.notificacionService = notificacionService;
        this.emailService = emailService;
    }

    // GET /api/pedidos/{pedidoId}/chat/{vendedorId} — mensajes del hilo, marca como leídos los del otro participante
    // @Transactional: marcarLeidosParaLector es una consulta @Modifying (UPDATE) y requiere una
    // transacción activa, si no Hibernate lanza TransactionRequiredException.
    @Transactional
    @GetMapping
    public ResponseEntity<?> listar(@PathVariable Long pedidoId, @PathVariable Long vendedorId, Authentication auth) {
        User usuario = resolverUsuario(auth);
        Pedido pedido = pedidoRepository.findByIdWithDetalles(pedidoId).orElse(null);
        if (pedido == null) return ResponseEntity.notFound().build();

        if (!autorizado(pedido, vendedorId, usuario)) {
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        }

        chatMensajeRepository.marcarLeidosParaLector(pedidoId, vendedorId, usuario.getId());

        List<Map<String, Object>> mensajes = chatMensajeRepository
                .findByPedidoIdAndVendedorIdOrderByFechaEnvioAsc(pedidoId, vendedorId)
                .stream()
                .map(this::toMap)
                .toList();

        return ResponseEntity.ok(mensajes);
    }

    // POST /api/pedidos/{pedidoId}/chat/{vendedorId} — envía un mensaje en el hilo
    @PostMapping
    public ResponseEntity<?> enviar(@PathVariable Long pedidoId, @PathVariable Long vendedorId,
                                     @RequestBody Map<String, Object> body, Authentication auth) {
        User usuario = resolverUsuario(auth);
        Pedido pedido = pedidoRepository.findByIdWithDetalles(pedidoId).orElse(null);
        if (pedido == null) return ResponseEntity.notFound().build();

        if (!autorizado(pedido, vendedorId, usuario)) {
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        }

        String contenido = body.get("contenido") instanceof String s ? s.trim() : null;
        if (contenido == null || contenido.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El mensaje no puede estar vacío"));
        }

        User vendedor = vendedorId.equals(usuario.getId()) ? usuario : userService.buscarPorId(vendedorId).orElse(null);
        if (vendedor == null) return ResponseEntity.notFound().build();

        ChatMensaje mensaje = new ChatMensaje();
        mensaje.setPedido(pedido);
        mensaje.setVendedor(vendedor);
        mensaje.setAutor(usuario);
        mensaje.setContenido(contenido);
        mensaje.setFechaEnvio(LocalDateTime.now());
        chatMensajeRepository.save(mensaje);

        User destinatario = usuario.getId().equals(pedido.getUsuario().getId()) ? vendedor : pedido.getUsuario();
        String resumen = contenido.length() > 80 ? contenido.substring(0, 80) + "…" : contenido;
        notificacionService.crear(
                destinatario,
                usuario.getUsername() + " te escribió sobre el pedido #" + pedidoId + ": " + resumen,
                "NUEVO_MENSAJE_CHAT",
                pedidoId,
                null
        );
        if (destinatario.getEmail() != null && !destinatario.getEmail().isBlank()) {
            emailService.enviarCorreo(
                    destinatario.getEmail(),
                    "Nuevo mensaje sobre tu pedido #" + pedidoId + " - MarketS",
                    usuario.getUsername() + " te escribió:\n\n" + contenido
            );
        }

        return ResponseEntity.ok(toMap(mensaje));
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
        return userService.buscarPorEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + auth.getName()));
    }

    private Map<String, Object> toMap(ChatMensaje m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        map.put("autorId", m.getAutor().getId());
        map.put("autorUsername", m.getAutor().getUsername());
        map.put("esVendedor", m.getAutor().getId().equals(m.getVendedor().getId()));
        map.put("contenido", m.getContenido());
        map.put("fechaEnvio", m.getFechaEnvio().toString());
        return map;
    }
}
