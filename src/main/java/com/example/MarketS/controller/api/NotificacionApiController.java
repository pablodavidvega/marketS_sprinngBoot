package com.example.MarketS.controller.api;

import com.example.MarketS.model.Notificacion;
import com.example.MarketS.model.User;
import com.example.MarketS.service.NotificacionService;
import com.example.MarketS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionApiController {

    private final NotificacionService notificacionService;
    private final UserService         userService;

    // GET /api/notificaciones — últimas 20 + conteo de no leídas
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(Authentication auth) {
        User user = resolverUsuario(auth);
        List<Map<String, Object>> items = notificacionService.listarRecientes(user.getId())
                .stream().map(this::toMap).toList();
        long noLeidas = notificacionService.contarNoLeidas(user.getId());
        return ResponseEntity.ok(Map.of("items", items, "noLeidas", noLeidas));
    }

    // PATCH /api/notificaciones/{id}/leer
    @PatchMapping("/{id}/leer")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id, Authentication auth) {
        User user = resolverUsuario(auth);
        notificacionService.marcarLeida(id, user.getId());
        return ResponseEntity.ok().build();
    }

    // PATCH /api/notificaciones/leer-todas
    @PatchMapping("/leer-todas")
    public ResponseEntity<Void> marcarTodasLeidas(Authentication auth) {
        User user = resolverUsuario(auth);
        notificacionService.marcarTodasLeidas(user.getId());
        return ResponseEntity.ok().build();
    }

    private User resolverUsuario(Authentication auth) {
        return userService.buscarPorEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private Map<String, Object> toMap(Notificacion n) {
        return Map.of(
            "id",            n.getId(),
            "mensaje",       n.getMensaje(),
            "tipo",          n.getTipo(),
            "leida",         n.isLeida(),
            "fechaCreacion", n.getFechaCreacion().toString(),
            "pedidoId",      n.getPedidoId() != null ? n.getPedidoId() : ""
        );
    }
}
