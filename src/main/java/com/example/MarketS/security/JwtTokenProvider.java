package com.example.MarketS.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String generateToken(Authentication authentication) {
        String email = authentication.getName();
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(email)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key())
                .compact();
    }

    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ── Pending signup token (login con Google) ──────────────────────────────
    // Token de corta vida que certifica que un email/googleId ya fue verificado contra
    // Google, mientras el usuario completa los datos que Google no provee (teléfono,
    // dirección, rol). Lleva claim "purpose" para distinguirse de un JWT de sesión real —
    // JwtAuthenticationFilter lo rechaza explícitamente si alguien lo presenta como cookie.
    private static final String PENDING_SIGNUP_PURPOSE = "google_signup";

    public String generatePendingSignupToken(String email, String googleId, String name, long ttlMillis) {
        return Jwts.builder()
                .subject(email)
                .claim("purpose", PENDING_SIGNUP_PURPOSE)
                .claim("googleId", googleId)
                .claim("name", name)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(key())
                .compact();
    }

    // Valida firma + expiración + que sea efectivamente un pending signup token; lanza
    // JwtException/IllegalArgumentException si no. El caller decide cómo traducir eso a HTTP.
    public Claims parsePendingSignupToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
        if (!PENDING_SIGNUP_PURPOSE.equals(claims.get("purpose", String.class))) {
            throw new JwtException("El token no es un pending signup token válido");
        }
        return claims;
    }

    // true si el token trae claim "purpose" (cualquiera) — un JWT de sesión normal
    // (generateToken) nunca lo lleva, así que esto identifica tokens que NO deben autenticar.
    public boolean hasPurposeClaim(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
            return claims.get("purpose", String.class) != null;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
