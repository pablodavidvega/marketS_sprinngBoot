package com.example.MarketS.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
public class GoogleTokenVerifierService {

    public record GooglePayload(String googleId, String email, boolean emailVerified, String name) {}

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierService(@Value("${google.oauth.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    // Verifica firma, issuer, audiencia y expiración del id_token contra las claves públicas
    // de Google. Lanza IllegalArgumentException si el token es inválido/expirado/de otra app.
    public GooglePayload verify(String idTokenString) throws GeneralSecurityException, IOException {
        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new IllegalArgumentException("Token de Google inválido o expirado");
        }
        GoogleIdToken.Payload payload = idToken.getPayload();
        return new GooglePayload(
                payload.getSubject(),
                payload.getEmail(),
                Boolean.TRUE.equals(payload.getEmailVerified()),
                (String) payload.get("name")
        );
    }
}
