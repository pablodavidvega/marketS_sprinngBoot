package com.example.MarketS.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Endpoint liviano para el monitor externo (UptimeRobot / cron-job.org) que visita el backend
// cada pocos minutos y evita que Render Free lo apague por inactividad. No toca la base de
// datos a propósito: cada visita sería una consulta a Supabase sin necesidad.
@RestController
@RequestMapping("/api/health")
public class HealthApiController {

    // @GetMapping también responde HEAD, que es lo que usan algunos monitores por defecto.
    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
