package com.example.MarketS.controller.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class AppConfigController {

    @Value("${app.nombre:MarketS}")
    private String nombreApp;

    @Value("${app.color.primario:#2563eb}")
    private String colorPrimario;

    @Value("${app.logo.url:}")
    private String logoUrl;

    @Value("${app.descripcion:Marketplace interno de tu empresa}")
    private String descripcion;

    @GetMapping("/app")
    public ResponseEntity<Map<String, String>> getConfig() {
        return ResponseEntity.ok(Map.of(
            "nombreApp",     nombreApp,
            "colorPrimario", colorPrimario,
            "logoUrl",       logoUrl,
            "descripcion",   descripcion
        ));
    }
}
