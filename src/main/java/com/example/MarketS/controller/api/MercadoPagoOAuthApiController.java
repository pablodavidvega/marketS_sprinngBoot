package com.example.MarketS.controller.api;

import com.example.MarketS.exception.RecursoNoEncontradoException;
import com.example.MarketS.model.User;
import com.example.MarketS.security.UsuarioPrincipal;
import com.example.MarketS.service.FrontendLinkBuilder;
import com.example.MarketS.service.MercadoPagoOAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

// Conexión de la cuenta de MercadoPago del vendedor (OAuth) — ver MercadoPagoOAuthService.
// El endpoint de callback es el único público de este controller: MP redirige el navegador del
// vendedor sin el header Authorization, así que no puede exigir sesión (ver SecurityConfig).
@RestController
public class MercadoPagoOAuthApiController {

    private final MercadoPagoOAuthService oauthService;
    private final FrontendLinkBuilder frontendLinkBuilder;

    public MercadoPagoOAuthApiController(MercadoPagoOAuthService oauthService,
                                          FrontendLinkBuilder frontendLinkBuilder) {
        this.oauthService = oauthService;
        this.frontendLinkBuilder = frontendLinkBuilder;
    }

    @PreAuthorize("hasAuthority('ROLE_VENDEDOR')")
    @PostMapping("/api/vendedor/mercadopago/conectar")
    public ResponseEntity<Map<String, String>> conectar(Authentication auth) {
        User vendedor = resolverUsuario(auth);
        String authUrl = oauthService.iniciarConexion(vendedor);
        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    @PreAuthorize("hasAuthority('ROLE_VENDEDOR')")
    @GetMapping("/api/vendedor/mercadopago/estado")
    public ResponseEntity<Map<String, Object>> estado(Authentication auth) {
        User vendedor = resolverUsuario(auth);
        boolean conectado = oauthService.estaConectado(vendedor);
        String mpEmail = oauthService.mpEmailConectado(vendedor).orElse(null);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("conectado", conectado);
        body.put("mpEmail", mpEmail);
        return ResponseEntity.ok(body);
    }

    @PreAuthorize("hasAuthority('ROLE_VENDEDOR')")
    @PostMapping("/api/vendedor/mercadopago/desconectar")
    public ResponseEntity<Void> desconectar(Authentication auth) {
        User vendedor = resolverUsuario(auth);
        oauthService.desconectar(vendedor);
        return ResponseEntity.ok().build();
    }

    // MP redirige acá el navegador del vendedor (sin Authorization) al terminar la autorización.
    @GetMapping("/api/mercadopago/oauth/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
                                          @RequestParam(required = false) String state) {
        boolean exito = code != null && state != null && oauthService.manejarCallback(code, state);
        return ResponseEntity.status(302)
                .location(URI.create(frontendLinkBuilder.mercadoPagoConectado(exito)))
                .build();
    }

    private User resolverUsuario(Authentication auth) {
        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) throw new RecursoNoEncontradoException("Usuario no encontrado: " + (auth != null ? auth.getName() : "anónimo"));
        return usuario;
    }
}
