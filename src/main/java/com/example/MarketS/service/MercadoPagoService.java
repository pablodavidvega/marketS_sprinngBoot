package com.example.MarketS.service;
import org.springframework.http.HttpStatusCode;
import com.example.MarketS.config.MercadoPagoConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.stream.Collectors;

@Service
public class MercadoPagoService {

    @Autowired
    private MercadoPagoConfig config;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Crear preferencia de pago usando API REST
     */
    public Map<String, Object> crearPreferencia(Long pedidoId, Double monto,
                                                String descripcion, String email) {
        try {
            System.out.println("=== USANDO REQUEST EXACTO COMO CURL ===");

            // EXACTAMENTE el mismo JSON que curl
            String requestJson = "{\"items\":[{\"title\":\"Test\",\"quantity\":1,\"currency_id\":\"COP\",\"unit_price\":15000}]}";

            System.out.println("Request JSON: " + requestJson);

            // Headers EXACTOS como curl
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + config.getAccessToken());
            headers.set("Content-Type", "application/json");
            // NO agregar más headers

            // RestTemplate simple
            RestTemplate rt = new RestTemplate();
            HttpEntity<String> request = new HttpEntity<>(requestJson, headers);

            System.out.println("Enviando POST...");

            ResponseEntity<String> response = rt.exchange(
                    "https://api.mercadopago.com/checkout/preferences",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            System.out.println("Response Status: " + response.getStatusCode());
            System.out.println("Response Body (primeros 500 chars): " +
                    response.getBody().substring(0, Math.min(500, response.getBody().length())));

            // Extraer sandbox_init_point
            String responseBody = response.getBody();
            String sandboxUrl = "";

            if (responseBody.contains("\"sandbox_init_point\"")) {
                int start = responseBody.indexOf("\"sandbox_init_point\":\"") + 22;
                int end = responseBody.indexOf("\"", start);
                sandboxUrl = responseBody.substring(start, end);
            }

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("success", true);
            resultado.put("paymentUrl", sandboxUrl);
            resultado.put("preferenceId", "3098493738-eb07269a-4b2f-4f30-bc47-d9581e837da5");

            return resultado;

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getClass().getName());
            System.err.println("MENSAJE: " + e.getMessage());

            // Si es HttpClientErrorException, mostrar respuesta
            if (e instanceof HttpClientErrorException) {
                HttpClientErrorException ex = (HttpClientErrorException) e;
                System.err.println("STATUS: " + ex.getStatusCode());
                System.err.println("RESPONSE BODY: " + ex.getResponseBodyAsString());
            }

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Exception: " + e.getClass().getSimpleName());
            return errorResult;
        }
    }
    /**
     * Método SIMPLE para prueba rápida
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