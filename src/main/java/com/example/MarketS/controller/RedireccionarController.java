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
                // Administradores van a su panel de administración
                return "redirect:/admin/dashboard";
            } else if (authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_VENDEDOR"))) {
                // Vendedores van a su panel de gestión
                return "redirect:/vendedor/dashboard";
            }
        }
        // Clientes y cualquier otro rol permanecen en la experiencia de compra
        return "redirect:/productos/index";
    }
}