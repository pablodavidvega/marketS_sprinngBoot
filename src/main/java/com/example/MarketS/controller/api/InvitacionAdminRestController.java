package com.example.MarketS.controller.api;

import com.example.MarketS.model.Invitacion;
import com.example.MarketS.model.User;
import com.example.MarketS.security.UsuarioPrincipal;
import com.example.MarketS.service.ConfiguracionAppService;
import com.example.MarketS.service.InvitacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/invitaciones")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class InvitacionAdminRestController {

    private final InvitacionService invitacionService;
    private final ConfiguracionAppService configService;

    // GET /api/admin/invitaciones — listar paginadas (30 por defecto), con filtro opcional por
    // estado (activa/expirada/agotada/revocada). Antes traía todas sin paginar.
    @GetMapping
    public ResponseEntity<Map<String, Object>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Page<Invitacion> pagina = invitacionService.listar(
                (estado == null || estado.isBlank()) ? null : estado,
                PageRequest.of(page, size));

        List<Map<String, Object>> contenido = pagina.getContent().stream()
                .map(this::toMap)
                .toList();

        return ResponseEntity.ok(Map.of(
                "contenido", contenido,
                "paginaActual", pagina.getNumber(),
                "totalPaginas", pagina.getTotalPages(),
                "totalElementos", pagina.getTotalElements(),
                "stats", invitacionService.stats()
        ));
    }

    // POST /api/admin/invitaciones — crear nueva
    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        User admin = UsuarioPrincipal.from(auth);
        if (admin == null) throw new RuntimeException("Admin no encontrado");

        String descripcion = (String) body.getOrDefault("descripcion", null);
        Integer maxUsos = body.get("maxUsos") != null
                ? Integer.valueOf(body.get("maxUsos").toString()) : null;
        LocalDateTime fechaExpiracion = body.get("fechaExpiracion") != null
                ? LocalDateTime.parse(body.get("fechaExpiracion").toString()) : null;

        Invitacion inv = invitacionService.crear(descripcion, maxUsos, fechaExpiracion, admin);
        return ResponseEntity.ok(toMap(inv));
    }

    // PATCH /api/admin/invitaciones/{id}/revocar
    @PatchMapping("/{id}/revocar")
    public ResponseEntity<Void> revocar(@PathVariable Long id) {
        invitacionService.revocar(id);
        return ResponseEntity.ok().build();
    }

    // PATCH /api/admin/invitaciones/{id}/reactivar
    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<Void> reactivar(@PathVariable Long id) {
        invitacionService.reactivar(id);
        return ResponseEntity.ok().build();
    }

    // DELETE /api/admin/invitaciones/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        invitacionService.eliminar(id);
        return ResponseEntity.ok().build();
    }

    // GET /api/admin/invitaciones/config
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        return ResponseEntity.ok(Map.of(
                "modo",    configService.getModoRegistro(),
                "dominio", configService.getDominioPermitido()
        ));
    }

    // PUT /api/admin/invitaciones/config
    @PutMapping("/config")
    public ResponseEntity<Void> setConfig(@RequestBody Map<String, String> body) {
        if (body.containsKey("modo"))    configService.set(ConfiguracionAppService.MODO_REGISTRO, body.get("modo"));
        if (body.containsKey("dominio")) configService.set(ConfiguracionAppService.DOMINIO_EMAIL, body.get("dominio"));
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> toMap(Invitacion inv) {
        return Map.of(
                "id",              inv.getId(),
                "token",           inv.getToken(),
                "descripcion",     inv.getDescripcion() != null ? inv.getDescripcion() : "",
                "activo",          inv.isActivo(),
                "vigente",         inv.isVigente(),
                "usosActuales",    inv.getUsosActuales(),
                "maxUsos",         inv.getMaxUsos() != null ? inv.getMaxUsos() : "",
                "fechaExpiracion", inv.getFechaExpiracion() != null ? inv.getFechaExpiracion().toString() : "",
                "fechaCreacion",   inv.getFechaCreacion().toString()
        );
    }
}
