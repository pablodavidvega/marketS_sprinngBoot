package com.example.MarketS.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Limitador de intentos en memoria (ventana fija) para los endpoints públicos de auth
// (login/registro/forgot-password). Alcanza con memoria local porque Render corre esta app en
// una sola instancia (ver render.yaml) — si en algún momento se escala a varias instancias, esto
// deja de ser efectivo y hay que mover el conteo a Redis o similar.
@Service
public class RateLimitService {

    private static final class Ventana {
        volatile long inicioMs;
        final AtomicInteger intentos = new AtomicInteger(0);
    }

    private final ConcurrentHashMap<String, Ventana> ventanas = new ConcurrentHashMap<>();

    public boolean permitirLogin(HttpServletRequest request, String email) {
        return consumir("login:ip:" + ip(request), 20, Duration.ofMinutes(15))
                && consumir("login:email:" + normalizar(email), 6, Duration.ofMinutes(15));
    }

    public boolean permitirRegistro(HttpServletRequest request) {
        return consumir("registro:ip:" + ip(request), 8, Duration.ofHours(1));
    }

    public boolean permitirForgotPassword(HttpServletRequest request, String email) {
        return consumir("forgot:ip:" + ip(request), 10, Duration.ofHours(1))
                && consumir("forgot:email:" + normalizar(email), 3, Duration.ofHours(1));
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private boolean consumir(String key, int maxIntentos, Duration ventanaDuracion) {
        long ahora = System.currentTimeMillis();
        long ventanaMs = ventanaDuracion.toMillis();
        Ventana v = ventanas.computeIfAbsent(key, k -> new Ventana());
        synchronized (v) {
            if (ahora - v.inicioMs >= ventanaMs) {
                v.inicioMs = ahora;
                v.intentos.set(0);
            }
            if (v.intentos.get() >= maxIntentos) {
                return false;
            }
            v.intentos.incrementAndGet();
            return true;
        }
    }

    // Primera IP de X-Forwarded-For (la del cliente real) — Render está detrás de un proxy que
    // reescribe getRemoteAddr() a la IP interna del proxy.
    private String ip(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // Evita que el mapa crezca indefinidamente con IPs/emails que no vuelven a intentar.
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void limpiarVentanasExpiradas() {
        long ahora = System.currentTimeMillis();
        long limiteMs = Duration.ofHours(2).toMillis();
        ventanas.entrySet().removeIf(e -> ahora - e.getValue().inicioMs > limiteMs);
    }
}
