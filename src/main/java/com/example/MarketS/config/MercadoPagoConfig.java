package com.example.MarketS.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MercadoPagoConfig {

    @Value("${mercado.pago.access.token}")
    private String accessToken;

    @Value("${app.url:http://localhost:8081}")
    private String appUrl;

    // Credenciales de la app de MercadoPago en modo Marketplace/OAuth ("MS2Version" en el panel
    // de desarrolladores de MP) — client_id es el "N° de aplicación", client_secret es el Access
    // Token de ESA MISMA app (según la doc de MP, el client_secret del flujo OAuth es el propio
    // Access Token de la aplicación dueña del client_id). Deliberadamente separado de
    // accessToken de arriba: ese es de otra app de MP más vieja, la que ya procesa los cobros de
    // Checkout Pro hoy — mezclar client_id de una app con el Access Token de otra hace fallar el
    // intercambio de token en MercadoPagoOAuthService.
    @Value("${mercado.pago.client.id:}")
    private String clientId;

    @Value("${mercado.pago.client.secret:}")
    private String clientSecret;

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

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    // Mismo host que ya usan los back_urls de Checkout Pro (ver PedidoApiController), así que no
    // hace falta una variable de entorno aparte: solo declarar esta URL en el panel de MP como
    // "URL de redireccionamiento" de la aplicación.
    public String getOauthRedirectUri() {
        return appUrl + "/api/mercadopago/oauth/callback";
    }
}