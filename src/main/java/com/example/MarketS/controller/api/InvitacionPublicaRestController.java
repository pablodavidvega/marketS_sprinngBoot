package com.example.MarketS.controller.api;

import com.example.MarketS.model.Invitacion;
import com.example.MarketS.service.ConfiguracionAppService;
import com.example.MarketS.service.InvitacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/invitaciones")
@RequiredArgsConstructor
public class InvitacionPublicaRestController {

    private final InvitacionService invitacionService;
    private final ConfiguracionAppService configService;

    // Estado del registro — el frontend lo consulta al cargar la página de registro
    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> estado() {
        String modo    = configService.getModoRegistro();
        String dominio = configService.getDominioPermitido();
        boolean abierto = ConfiguracionAppService.MODO_ABIERTO.equals(modo);
        return ResponseEntity.ok(Map.of(
                "modo",             modo,
                "dominioPermitido", dominio,
                "registroAbierto",  abierto
        ));
    }

    // Validar token de invitación — el frontend lo llama al detectar ?invitacion=TOKEN en la URL
    @GetMapping("/validar")
    public ResponseEntity<Map<String, Object>> validar(@RequestParam String token) {
        Optional<Invitacion> opt = invitacionService.buscarPorToken(token);
        if (opt.isEmpty() || !opt.get().isVigente()) {
            return ResponseEntity.ok(Map.of(
                    "valido",      false,
                    "mensaje",     "El link de invitación no es válido o ha expirado"
            ));
        }
        Invitacion inv = opt.get();
        return ResponseEntity.ok(Map.of(
                "valido",      true,
                "descripcion", inv.getDescripcion() != null ? inv.getDescripcion() : "",
                "mensaje",     "Invitación válida"
        ));
    }
}
