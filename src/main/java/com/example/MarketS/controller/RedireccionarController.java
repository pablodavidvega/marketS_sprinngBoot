package com.example.MarketS.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Collection;

@Controller
public class RedireccionarController {

    @GetMapping("/redireccionar-por-rol")
    public String redireccionarPorRol(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

            if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return "redirect:/admin/dashboard";
            } else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_VENDEDOR"))) {
                return "redirect:/vendedor/dashboard";
            }
        }
        // Por defecto redirige al dashboard de cliente
        return "redirect:/cliente/dashboard";
    }
}