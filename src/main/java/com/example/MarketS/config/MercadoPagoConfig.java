package com.example.MarketS.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MercadoPagoConfig {

    @Value("${mercado.pago.access.token}")
    private String accessToken;

    @Value("${app.url:http://localhost:8081}")
    private String appUrl;

    public MercadoPagoConfig() {
        System.out.println("==================================================");
        System.out.println("CONFIGURACION MERCADO PAGO - MODO API REST");
        System.out.println("Token cargado: " + (accessToken != null ?
                "SI (" + accessToken.substring(0, Math.min(10, accessToken.length())) + "...)" : "NO"));
        System.out.println("App URL: " + appUrl);
        System.out.println("==================================================");
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getAppUrl() {
        return appUrl;
    }
}