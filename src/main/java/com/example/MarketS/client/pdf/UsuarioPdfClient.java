package com.example.MarketS.client.pdf;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class UsuarioPdfClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String url = "http://localhost:8081/api/usuarios/estadisticas";

    public Map<String, Object> consumirEstadisticas() {
        return restTemplate.getForObject(url, Map.class);
    }
}
