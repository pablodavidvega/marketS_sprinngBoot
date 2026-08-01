package com.example.MarketS.controller.api;

import com.example.MarketS.model.Notificacion;
import com.example.MarketS.model.User;
import com.example.MarketS.security.UsuarioPrincipal;
import com.example.MarketS.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionApiController {

    private final NotificacionService notificacionService;

    // GET /api/notificaciones — últimas 20 + conteo de no leídas
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(Authentication auth) {
        User user = resolverUsuario(auth);
        List<Map<String, Object>> items = notificacionService.listarRecientes(user.getId())
                .stream().map(this::toMap).toList();
        long noLeidas = notificacionService.contarNoLeidas(user.getId());
        return ResponseEntity.ok(Map.of("items", items, "noLeidas", noLeidas));
    }

    // GET /api/notificaciones/todas — historial completo paginado, para la página dedicada
    // (a diferencia de /api/notificaciones, que solo trae las últimas 20 para el dropdown)
    @GetMapping("/todas")
    public ResponseEntity<Map<String, Object>> listarTodas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean soloNoLeidas,
            Authentication auth) {
        User user = resolverUsuario(auth);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        Page<Notificacion> pagina = notificacionService.listarPaginado(user.getId(), soloNoLeidas, pageable);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contenido", pagina.getContent().stream().map(this::toMap).toList());
        body.put("paginaActual", pagina.getNumber());
        body.put("totalPaginas", pagina.getTotalPages());
        body.put("totalElementos", pagina.getTotalElements());
        return ResponseEntity.ok(body);
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
        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) throw new RuntimeException("Usuario no encontrado");
        return usuario;
    }

    private Map<String, Object> toMap(Notificacion n) {
        return Map.of(
            "id",            n.getId(),
            "mensaje",       n.getMensaje(),
            "tipo",          n.getTipo(),
            "leida",         n.isLeida(),
            "fechaCreacion", n.getFechaCreacion().toString(),
            "pedidoId",      n.getPedidoId() != null ? n.getPedidoId() : "",
            "productoId",    n.getProductoId() != null ? n.getProductoId() : "",
            "vendedorId",    n.getVendedorId() != null ? n.getVendedorId() : "",
            "rolDestino",    n.getRolDestino() != null ? n.getRolDestino() : ""
        );
    }
}
