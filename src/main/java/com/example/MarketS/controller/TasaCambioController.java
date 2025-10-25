// src/main/java/com/example/MarketS/controller/TasaCambioController.java
package com.example.MarketS.controller;

import com.example.MarketS.service.TasaCambioService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasas")
@CrossOrigin(origins = "*")
public class TasaCambioController {

    private final TasaCambioService tasaCambioService;

    public TasaCambioController(TasaCambioService tasaCambioService) {
        this.tasaCambioService = tasaCambioService;
    }

    @GetMapping("/usd-cop")
    public String obtenerTasaUsdCop() {
        Double tasa = tasaCambioService.obtenerTasaDolarACOP();
        return "💵 1 USD = " + tasa + " COP";
    }
}
