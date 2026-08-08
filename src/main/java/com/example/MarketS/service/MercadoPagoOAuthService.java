package com.example.MarketS.service;

import com.example.MarketS.config.MercadoPagoConfig;
import com.example.MarketS.model.User;
import com.example.MarketS.model.VendedorMercadoPago;
import com.example.MarketS.repository.VendedorMercadoPagoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

// Conecta la cuenta de MercadoPago de cada vendedor vía OAuth (grant type "Authorization code",
// modo Marketplace) para que el dinero de sus ventas vaya directo a su cuenta — ver
// VendedorMercadoPago y MercadoPagoOAuthApiController. El client_secret que exige el flujo OAuth
// de MP es, según su propia documentación, el mismo Access Token de la app (MercadoPagoConfig).
@Service
public class MercadoPagoOAuthService {

    private static final int STATE_VIGENCIA_MINUTOS = 10;
    private static final long EXPIRACION_POR_DEFECTO_SEGUNDOS = 180L * 24 * 60 * 60; // 180 días

    private final VendedorMercadoPagoRepository repository;
    private final MercadoPagoConfig config;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MercadoPagoOAuthService(VendedorMercadoPagoRepository repository, MercadoPagoConfig config) {
        this.repository = repository;
        this.config = config;
    }

    // Arranca (o reinicia) el flujo de conexión de un vendedor: genera un state de un solo uso y
    // devuelve la URL de autorización de MP a la que hay que redirigir su navegador.
    public String iniciarConexion(User vendedor) {
        VendedorMercadoPago cuenta = repository.findByUser(vendedor).orElseGet(() -> {
            VendedorMercadoPago nueva = new VendedorMercadoPago();
            nueva.setUser(vendedor);
            return nueva;
        });
        String state = UUID.randomUUID().toString();
        cuenta.setState(state);
        cuenta.setStateCreadoEn(LocalDateTime.now());
        repository.save(cuenta);

        return "https://auth.mercadopago.com/authorization"
                + "?client_id=" + config.getClientId()
                + "&response_type=code"
                + "&platform_id=mp"
                + "&state=" + state
                + "&redirect_uri=" + config.getOauthRedirectUri();
    }

    // Recibe el code+state que MP devuelve por el redirect_uri, los cambia por un access_token
    // propio del vendedor y los guarda. Devuelve true si la conexión quedó completa.
    //
    // Logueado paso a paso (antes solo se sabía "falló", sin decir en qué punto ni por qué —
    // ver también llamarOauthToken, que antes descartaba el body de error de MP) porque el
    // único síntoma visible para el vendedor es un genérico "No se pudo completar la conexión"
    // en /vendedor/pagos, sin forma de distinguir state vencido de credenciales mal
    // configuradas (client_id/client_secret de la app OAuth, ver comentario en
    // MercadoPagoConfig) o de un error real de la API de MP.
    public boolean manejarCallback(String code, String state) {
        if (state == null || state.isBlank()) {
            System.err.println("MercadoPago OAuth callback: falta el parámetro state");
            return false;
        }
        Optional<VendedorMercadoPago> cuentaOpt = repository.findByState(state);
        if (cuentaOpt.isEmpty()) {
            System.err.println("MercadoPago OAuth callback: state no encontrado (¿ya se usó o nunca se generó?): " + state);
            return false;
        }

        VendedorMercadoPago cuenta = cuentaOpt.get();
        if (cuenta.getStateCreadoEn() == null
                || cuenta.getStateCreadoEn().isBefore(LocalDateTime.now().minusMinutes(STATE_VIGENCIA_MINUTOS))) {
            System.err.println("MercadoPago OAuth callback: state vencido para vendedor " + cuenta.getUser().getId());
            return false;
        }

        JsonNode tokenResponse = intercambiarCodePorToken(code);
        if (tokenResponse == null) {
            System.err.println("MercadoPago OAuth callback: falló el intercambio code→token para vendedor " + cuenta.getUser().getId());
            return false;
        }

        aplicarTokenResponse(cuenta, tokenResponse);
        cuenta.setMpEmail(obtenerEmailMp(cuenta.getAccessToken()));
        cuenta.setState(null);
        cuenta.setStateCreadoEn(null);
        cuenta.setConectadoEn(LocalDateTime.now());
        repository.save(cuenta);
        return true;
    }

    // Token vigente para crear/verificar pagos a nombre del vendedor; lo renueva solo si venció.
    public Optional<String> obtenerAccessTokenVigente(User vendedor) {
        Optional<VendedorMercadoPago> cuentaOpt = repository.findByUser(vendedor);
        if (cuentaOpt.isEmpty() || !cuentaOpt.get().estaConectado()) return Optional.empty();

        VendedorMercadoPago cuenta = cuentaOpt.get();
        if (cuenta.getExpiraEn() != null && cuenta.getExpiraEn().isAfter(LocalDateTime.now())) {
            return Optional.of(cuenta.getAccessToken());
        }

        JsonNode refreshResponse = refrescarToken(cuenta.getRefreshToken());
        if (refreshResponse == null) return Optional.empty();

        aplicarTokenResponse(cuenta, refreshResponse);
        repository.save(cuenta);
        return Optional.of(cuenta.getAccessToken());
    }

    public void desconectar(User vendedor) {
        repository.findByUser(vendedor).ifPresent(repository::delete);
    }

    public boolean estaConectado(User vendedor) {
        return repository.findByUser(vendedor).map(VendedorMercadoPago::estaConectado).orElse(false);
    }

    public Optional<String> mpEmailConectado(User vendedor) {
        return repository.findByUser(vendedor)
                .filter(VendedorMercadoPago::estaConectado)
                .map(VendedorMercadoPago::getMpEmail);
    }

    private JsonNode intercambiarCodePorToken(String code) {
        String body = "{"
                + "\"client_id\":\"" + config.getClientId() + "\","
                + "\"client_secret\":\"" + config.getClientSecret() + "\","
                + "\"grant_type\":\"authorization_code\","
                + "\"code\":\"" + escapeJson(code) + "\","
                + "\"redirect_uri\":\"" + config.getOauthRedirectUri() + "\""
                + "}";
        return llamarOauthToken(body);
    }

    private JsonNode refrescarToken(String refreshToken) {
        if (refreshToken == null) return null;
        String body = "{"
                + "\"client_id\":\"" + config.getClientId() + "\","
                + "\"client_secret\":\"" + config.getClientSecret() + "\","
                + "\"grant_type\":\"refresh_token\","
                + "\"refresh_token\":\"" + escapeJson(refreshToken) + "\""
                + "}";
        return llamarOauthToken(body);
    }

    private JsonNode llamarOauthToken(String requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.mercadopago.com/oauth/token", HttpMethod.POST, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                System.err.println("MercadoPago oauth/token respondió " + response.getStatusCode() + ": " + response.getBody());
                return null;
            }
            return objectMapper.readTree(response.getBody());
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // 4xx/5xx: acá viene el motivo real de MP (invalid_client, invalid_grant,
            // redirect_uri mismatch, etc.) — RestTemplate lo tira como excepción en vez de
            // devolverlo en response, y antes se perdía porque solo se logueaba e.getMessage().
            System.err.println("Error en oauth/token de MercadoPago (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            System.err.println("Error en oauth/token de MercadoPago: " + e.getMessage());
            return null;
        }
    }

    private String obtenerEmailMp(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.mercadopago.com/users/me", HttpMethod.GET, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return null;
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("email").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void aplicarTokenResponse(VendedorMercadoPago cuenta, JsonNode tokenResponse) {
        cuenta.setAccessToken(tokenResponse.path("access_token").asText());
        cuenta.setRefreshToken(tokenResponse.path("refresh_token").asText());
        cuenta.setMpUserId(tokenResponse.path("user_id").asText(null));
        long expiresIn = tokenResponse.path("expires_in").asLong(EXPIRACION_POR_DEFECTO_SEGUNDOS);
        cuenta.setExpiraEn(LocalDateTime.now().plusSeconds(expiresIn));
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
