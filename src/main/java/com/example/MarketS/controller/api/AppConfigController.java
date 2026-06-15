package com.example.MarketS.controller.api;

import com.example.MarketS.service.ConfiguracionAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class AppConfigController {

    private static final String KEY_NOMBRE      = "app.nombre";
    private static final String KEY_COLOR       = "app.color";
    private static final String KEY_LOGO        = "app.logo";
    private static final String KEY_DESCRIPCION = "app.descripcion";

    private final ConfiguracionAppService configuracionAppService;

    @Value("${app.nombre:MarketS}")
    private String nombreDefault;

    @Value("${app.color.primario:#2563eb}")
    private String colorDefault;

    @Value("${app.logo.url:}")
    private String logoDefault;

    @Value("${app.descripcion:Marketplace interno de tu empresa}")
    private String descripcionDefault;

    @GetMapping("/app")
    public ResponseEntity<Map<String, String>> getConfig() {
        return ResponseEntity.ok(Map.of(
            "nombreApp",     configuracionAppService.get(KEY_NOMBRE,      nombreDefault),
            "colorPrimario", configuracionAppService.get(KEY_COLOR,       colorDefault),
            "logoUrl",       configuracionAppService.get(KEY_LOGO,        logoDefault),
            "descripcion",   configuracionAppService.get(KEY_DESCRIPCION, descripcionDefault)
        ));
    }

    @PutMapping("/app")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> updateConfig(@RequestBody Map<String, String> body) {
        if (body.containsKey("nombreApp"))     configuracionAppService.set(KEY_NOMBRE,      body.get("nombreApp").strip());
        if (body.containsKey("colorPrimario")) configuracionAppService.set(KEY_COLOR,       body.get("colorPrimario").strip());
        if (body.containsKey("logoUrl"))       configuracionAppService.set(KEY_LOGO,        body.get("logoUrl").strip());
        if (body.containsKey("descripcion"))   configuracionAppService.set(KEY_DESCRIPCION, body.get("descripcion").strip());
        return getConfig();
    }

    @DeleteMapping("/app")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> resetConfig() {
        configuracionAppService.delete(KEY_NOMBRE);
        configuracionAppService.delete(KEY_COLOR);
        configuracionAppService.delete(KEY_LOGO);
        configuracionAppService.delete(KEY_DESCRIPCION);
        return ResponseEntity.ok(Map.of(
            "nombreApp",     nombreDefault,
            "colorPrimario", colorDefault,
            "logoUrl",       logoDefault,
            "descripcion",   descripcionDefault
        ));
    }
}
