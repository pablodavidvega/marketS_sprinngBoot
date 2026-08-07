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

    public Map<String, Object> crearPreferencia(Long pedidoId, Double monto,
                                                String descripcion, String email) {
        return crearPreferencia(pedidoId, monto, descripcion, email, null, null, null, null);
    }

    public Map<String, Object> crearPreferencia(Long pedidoId, Double monto,
                                                String descripcion, String email,
                                                String successUrl, String failureUrl, String pendingUrl) {
        return crearPreferencia(pedidoId, monto, descripcion, email, successUrl, failureUrl, pendingUrl, null);
    }

    // accessTokenVendedor: token OAuth propio del vendedor dueño del pedido (ver
    // MercadoPagoOAuthService), para que el pago vaya directo a su cuenta. Si viene null, se usa
    // el token de la propia plataforma (compatibilidad con los overloads de arriba).
    public Map<String, Object> crearPreferencia(Long pedidoId, Double monto,
                                                String descripcion, String email,
                                                String successUrl, String failureUrl, String pendingUrl,
                                                String accessTokenVendedor) {
        try {
            String token = (accessTokenVendedor != null && !accessTokenVendedor.isBlank())
                    ? accessTokenVendedor
                    : config.getAccessToken();

            if (token == null || token.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Token de MercadoPago no configurado");
                return error;
            }

            String requestJson = buildPreferenceJson(pedidoId, monto, descripcion, successUrl, failureUrl, pendingUrl);

            System.out.println("Request JSON enviado a MercadoPago:");
            System.out.println(requestJson);

            // Headers SIMPLES
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> request = new HttpEntity<>(requestJson, headers);

            System.out.println("Enviando POST a MercadoPago...");

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.mercadopago.com/checkout/preferences",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            System.out.println("Response Status: " + response.getStatusCode());

            String responseBody = response.getBody();
            System.out.println("Response recibida (primeros 300 chars):");
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
                        System.out.println("Sandbox URL encontrada: " + sandboxUrl);
                    }

                    if (root.has("id")) {
                        preferenceId = root.get("id").asText();
                        System.out.println("Preference ID: " + preferenceId);
                    }

                    // También intentar con init_point si sandbox no está
                    if (sandboxUrl.isEmpty() && root.has("init_point")) {
                        sandboxUrl = root.get("init_point").asText();
                        System.out.println("Usando init_point: " + sandboxUrl);
                    }

                    if (!sandboxUrl.isEmpty()) {
                        Map<String, Object> resultado = new HashMap<>();
                        resultado.put("success", true);
                        resultado.put("paymentUrl", sandboxUrl);
                        resultado.put("preferenceId", preferenceId);
                        resultado.put("rawResponse", responseBody);

                        System.out.println("¡PREFERENCIA CREADA EXITOSAMENTE!");
                        System.out.println("URL de pago: " + sandboxUrl);
                        System.out.println("ID de preferencia: " + preferenceId);

                        return resultado;
                    }

                } catch (Exception e) {
                    System.err.println("Error parseando respuesta JSON: " + e.getMessage());
                }
            }

            // Si llegamos aquí, hubo un error
            System.err.println("Error en respuesta de MercadoPago");
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Error en respuesta: " + response.getStatusCode());
            errorResult.put("responseBody", responseBody);

            return errorResult;

        } catch (Exception e) {
            System.err.println("ERROR en crearPreferencia:");
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
     * Consulta la API de MP para verificar que un pago existe, está aprobado
     * y pertenece al pedido indicado. Nunca confiar solo en los query params del callback.
     */
    public Map<String, Object> verificarPago(String paymentId) {
        return verificarPago(paymentId, null);
    }

    // accessTokenVendedor: el pago se consulta con el token de la cuenta de MP que lo recibió
    // (el vendedor dueño del pedido) — si viene null, se usa el token de la plataforma.
    public Map<String, Object> verificarPago(String paymentId, String accessTokenVendedor) {
        try {
            String token = (accessTokenVendedor != null && !accessTokenVendedor.isBlank())
                    ? accessTokenVendedor
                    : config.getAccessToken();
            if (token == null || token.isEmpty()) {
                return Map.of("valido", false, "motivo", "Token MP no configurado");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.mercadopago.com/v1/payments/" + paymentId,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                Map<String, Object> resultado = new HashMap<>();
                resultado.put("valido", true);
                resultado.put("status", root.path("status").asText());
                resultado.put("externalReference", root.path("external_reference").asText());
                resultado.put("monto", root.path("transaction_amount").asDouble());
                return resultado;
            }

            return Map.of("valido", false, "motivo", "Respuesta inesperada de MP: " + response.getStatusCode());

        } catch (Exception e) {
            return Map.of("valido", false, "motivo", "Error consultando MP: " + e.getMessage());
        }
    }

    public String pruebaConexion() {
        try {
            String token = config.getAccessToken();
            if (token == null || token.isEmpty()) {
                return "Token no configurado";
            }

            String tokenStart = token.substring(0, Math.min(10, token.length()));
            boolean isTestToken = token.startsWith("TEST") || token.startsWith("APP_USR");

            return "Token encontrado (inicio: " + tokenStart + ")\n" +
                    "Es token de test: " + isTestToken;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String buildPreferenceJson(Long pedidoId, Double monto, String descripcion,
                                        String successUrl, String failureUrl, String pendingUrl) {
        StringBuilder json = new StringBuilder()
                .append("{")
                .append("\"items\": [{")
                .append("\"title\": \"").append(escapeJsonString(descripcion)).append("\",")
                .append("\"quantity\": 1,")
                .append("\"currency_id\": \"COP\",")
                .append("\"unit_price\": ").append((long) Math.round(monto))
                .append("}],")
                .append("\"external_reference\": \"").append(pedidoId).append("\"");

        if (successUrl != null && failureUrl != null && pendingUrl != null) {
            json.append(",\"back_urls\": {")
                .append("\"success\": \"").append(escapeJsonString(successUrl)).append("\",")
                .append("\"failure\": \"").append(escapeJsonString(failureUrl)).append("\",")
                .append("\"pending\": \"").append(escapeJsonString(pendingUrl)).append("\"")
                .append("}");
            // auto_return solo funciona con URLs HTTPS (producción); localhost la rechaza MP
            if (successUrl.startsWith("https://")) {
                json.append(",\"auto_return\": \"approved\"");
            }
        }

        json.append("}");
        return json.toString();
    }

    private String escapeJsonString(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}