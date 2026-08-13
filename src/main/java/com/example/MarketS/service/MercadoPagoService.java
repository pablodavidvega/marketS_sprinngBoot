package com.example.MarketS.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class MercadoPagoService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // accessTokenVendedor: token OAuth propio del vendedor dueño del pedido (ver
    // MercadoPagoOAuthService), para que el pago vaya directo a su cuenta.
    public Map<String, Object> crearPreferencia(Long pedidoId, Double monto,
                                                String descripcion, String email,
                                                String successUrl, String failureUrl, String pendingUrl,
                                                String accessTokenVendedor) {
        try {
            if (accessTokenVendedor == null || accessTokenVendedor.isBlank()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Token de MercadoPago no configurado");
                return error;
            }
            String token = accessTokenVendedor;

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

                    String paymentUrl = "";
                    String preferenceId = "";

                    if (root.has("id")) {
                        preferenceId = root.get("id").asText();
                        System.out.println("Preference ID: " + preferenceId);
                    }

                    // init_point es el checkout real de MercadoPago; se prioriza sobre
                    // sandbox_init_point (antes era al revés) porque mandar una compra real al
                    // checkout de pruebas muestra el banner "Sandbox ¿cómo usarlo?" y ahí métodos
                    // como transferencia bancaria/PSE no redirigen a ningún banco real ya que no
                    // hay conexión bancaria de verdad en modo sandbox. Solo se cae a sandbox si MP
                    // no devolvió init_point (pasa con tokens de test TEST-...).
                    if (root.has("init_point")) {
                        paymentUrl = root.get("init_point").asText();
                        System.out.println("Checkout URL (producción): " + paymentUrl);
                    } else if (root.has("sandbox_init_point")) {
                        paymentUrl = root.get("sandbox_init_point").asText();
                        System.out.println("Checkout URL (sandbox, no vino init_point): " + paymentUrl);
                    }

                    if (!paymentUrl.isEmpty()) {
                        Map<String, Object> resultado = new HashMap<>();
                        resultado.put("success", true);
                        resultado.put("paymentUrl", paymentUrl);
                        resultado.put("preferenceId", preferenceId);
                        resultado.put("rawResponse", responseBody);

                        System.out.println("¡PREFERENCIA CREADA EXITOSAMENTE!");
                        System.out.println("URL de pago: " + paymentUrl);
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
    // accessTokenVendedor: el pago se consulta con el token de la cuenta de MP que lo recibió
    // (el vendedor dueño del pedido).
    public Map<String, Object> verificarPago(String paymentId, String accessTokenVendedor) {
        try {
            if (accessTokenVendedor == null || accessTokenVendedor.isBlank()) {
                return Map.of("valido", false, "motivo", "Token MP no configurado");
            }
            String token = accessTokenVendedor;

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