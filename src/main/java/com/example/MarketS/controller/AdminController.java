package com.example.MarketS.controller;

import com.example.MarketS.model.User;
import com.example.MarketS.service.UserService;
import com.example.MarketS.repository.RolRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final RolRepository rolRepository;

    public AdminController(UserService userService, RolRepository rolRepository) {
        this.userService = userService;
        this.rolRepository = rolRepository;
    }

    // Dashboard principal
    @GetMapping("/dashboard")
    public String mostrarDashboardAdmin(Authentication authentication, Model model) {
        String email = authentication.getName();
        User admin = userService.buscarPorEmail(email).orElse(null);

        model.addAttribute("admin", admin);
        model.addAttribute("activePage", "dashboard");

        // Datos para estadísticas
        model.addAttribute("totalUsuarios", userService.listarTodos().size());
        model.addAttribute("totalClientes", userService.listarPorRol("ROLE_CLIENTE").size());
        model.addAttribute("totalVendedores", userService.listarPorRol("ROLE_VENDEDOR").size());

        return "admin/dashboard";
    }


}
