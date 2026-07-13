package com.example.MarketS.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

// Construye la cookie httpOnly "auth_token" con la misma configuración (secure/sameSite) que
// usan todos los puntos de emisión de sesión: login clásico y login/registro con Google.
@Component
public class AuthCookieService {

    private static final String COOKIE_NAME = "auth_token";

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${cookie.samesite:Lax}")
    private String cookieSameSite;

    public ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
