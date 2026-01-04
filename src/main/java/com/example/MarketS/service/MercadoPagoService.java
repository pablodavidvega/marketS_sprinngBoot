package com.example.MarketS.service;

import com.example.MarketS.config.MercadoPagoConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class MercadoPagoService {

    @Autowired
    private MercadoPagoConfig config;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Crear preferencia de pago usando API REST - VERSIÓN SIMPLIFICADA
     */
    public Map<String, Object> crearPreferencia(Long pedidoId, Double monto,
                                                String descripcion, String email) {
        try {
            System.out.println("=== 🚀 MERCADO PAGO SERVICE: crearPreferencia ===");
            System.out.println("Pedido ID: " + pedidoId);
            System.out.println("Monto: $" + monto);
            System.out.println("Descripción: " + descripcion);
            System.out.println("Email: " + email);

            // Verificar token
            String token = config.getAccessToken();
            System.out.println("Token (primeros 20 chars): " +
                    (token != null ? token.substring(0, Math.min(20, token.length())) : "NULL"));

            if (token == null || token.isEmpty()) {
                System.err.println("❌ Token de MercadoPago no configurado");
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Token de MercadoPago no configurado");
                return error;
            }

            // URL base
            String baseUrl = config.getAppUrl();
            System.out.println("Base URL: " + baseUrl);

            // JSON SIMPLIFICADO - Lo mismo que funciona en tus pruebas
            String requestJson = "{"
                    + "\"items\": [{"
                    + "\"title\": \"" + escapeJsonString(descripcion) + "\","
                    + "\"quantity\": 1,"
                    + "\"currency_id\": \"COP\","
                    + "\"unit_price\": " + monto
                    + "}]"
                    + "}";

            System.out.println("📤 Request JSON enviado a MercadoPago:");
            System.out.println(requestJson);

            // Headers SIMPLES - igual que en tus pruebas exitosas
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> request = new HttpEntity<>(requestJson, headers);

            System.out.println("🔄 Enviando POST a MercadoPago...");

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.mercadopago.com/checkout/preferences",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            System.out.println("✅ Response Status: " + response.getStatusCode());

            String responseBody = response.getBody();
            System.out.println("📥 Response recibida (primeros 300 chars):");
            System.out.println(responseBody != null ?
                    responseBody.substring(0, Math.min(300, responseBody.length())) : "NULL");

            // Procesar respuesta
            if (response.getStatusCode().is2xxSuccessful() && responseBody != null) {
                try {
                    // Parsear JSON de respuesta
                    JsonNode root = objectMapper.readTree(responseBody);

                    String sandboxUrl = "";
                    String preferenceId = "";

                    if (root.has("sandbox_init_point")) {
                        sandboxUrl = root.get("sandbox_init_point").asText();
                        System.out.println("✅ Sandbox URL encontrada: " + sandboxUrl);
                    }

                    if (root.has("id")) {
                        preferenceId = root.get("id").asText();
                        System.out.println("✅ Preference ID: " + preferenceId);
                    }

                    // También intentar con init_point si sandbox no está
                    if (sandboxUrl.isEmpty() && root.has("init_point")) {
                        sandboxUrl = root.get("init_point").asText();
                        System.out.println("✅ Usando init_point: " + sandboxUrl);
                    }

                    if (!sandboxUrl.isEmpty()) {
                        Map<String, Object> resultado = new HashMap<>();
                        resultado.put("success", true);
                        resultado.put("paymentUrl", sandboxUrl);
                        resultado.put("preferenceId", preferenceId);
                        resultado.put("rawResponse", responseBody);

                        System.out.println("🎉 ¡PREFERENCIA CREADA EXITOSAMENTE!");
                        return resultado;
                    }

                } catch (Exception e) {
                    System.err.println("❌ Error parseando respuesta JSON: " + e.getMessage());
                }
            }

            // Si llegamos aquí, hubo un error
            System.err.println("❌ Error en respuesta de MercadoPago");
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Error en respuesta: " + response.getStatusCode());
            errorResult.put("responseBody", responseBody);

            return errorResult;

        } catch (Exception e) {
            System.err.println("❌ ERROR en crearPreferencia:");
            System.err.println("Tipo: " + e.getClass().getName());
            System.err.println("Mensaje: " + e.getMessage());

            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException ex = (HttpClientErrorException) e;
                System.err.println("Status: " + ex.getStatusCode());
                System.err.println("Response: " + ex.getResponseBodyAsString());
            }

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * Método para escapar strings en JSON (renombrado para evitar conflicto)
     */
    private String escapeJsonString(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Método SIMPLE para prueba rápida (mantener compatibilidad)
     */
    public String pruebaConexion() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + config.getAccessToken());

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.mercadopago.com/v1/payment_methods",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            return "Conexion exitosa. Status: " + response.getStatusCode();

        } catch (Exception e) {
            return "Error conexion: " + e.getMessage();
        }
    }
}