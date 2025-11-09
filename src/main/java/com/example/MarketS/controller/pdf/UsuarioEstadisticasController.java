package com.example.MarketS.controller.pdf;

import com.example.MarketS.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioEstadisticasController {

    private final UserService userService;

    public UsuarioEstadisticasController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/estadisticas")
    public Map<String, Object> estadisticasUsuarios() {
        long totalUsuarios = userService.listarTodos().size();
        long admin = userService.listarPorRol("ROLE_ADMIN").size();
        long cliente = userService.listarPorRol("ROLE_CLIENTE").size();
        long vendedor = userService.listarPorRol("ROLE_VENDEDOR").size();

        return Map.of(
                "Total Usuarios", totalUsuarios,
                "Admins", admin,
                "Clientes", cliente,
                "Vendedores", vendedor
        );
    }
}
