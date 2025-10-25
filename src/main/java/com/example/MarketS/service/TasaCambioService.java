package com.example.MarketS.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class TasaCambioService {

    public Double obtenerTasaDolarACOP() {
        String url = "https://open.er-api.com/v6/latest/USD";
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        Map<String, Double> rates = (Map<String, Double>) response.get("rates");

        return rates.get("COP"); // tasa del dólar a pesos colombianos
    }
}
