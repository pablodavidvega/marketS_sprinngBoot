package com.example.MarketS.controller;

import com.example.MarketS.dto.ClienteDto;
import com.example.MarketS.model.User;
import com.example.MarketS.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cliente")
public class ClienteController {
    //Inyeccion dependencias//
    private final UserService userService;
    //Constructor//
    public ClienteController(UserService userService) {
        this.userService = userService;
    }
    //Paths cliente//
    @GetMapping("/dashboard")
    public String mostrarDashboardCliente(Authentication authentication, Model model) {
        String email = authentication.getName();
        User usuario = userService.buscarPorEmail(email).orElse(null);
        model.addAttribute("usuario", usuario);
        return "cliente/dashboard";
    }

    @GetMapping("/editar-perfil")
    public String mostrarFormularioEditarPerfil(Model model, Authentication authentication) {
        String email = authentication.getName();
        User user = userService.buscarPorEmail(email).orElse(null);

        if (user != null) {
            ClienteDto clienteDto = new ClienteDto();
            clienteDto.setUsername(user.getUsername());
            clienteDto.setEmail(user.getEmail());
            clienteDto.setPhone(user.getPhone());
            clienteDto.setAddress(user.getAddress());

            model.addAttribute("clienteDto", clienteDto);
            return "cliente/editarPerfilCliente";
        }

        return "redirect:/cliente/dashboard"; // o muestra un error
    }

    @PostMapping("/editar-perfil")
    public String actualizarPerfil(@Valid @ModelAttribute("clienteDto") ClienteDto clienteDto,
                                   BindingResult result,
                                   Model model,
                                   Authentication authentication,
                                   HttpServletRequest request) {

        if (result.hasErrors()) {
            model.addAttribute("clienteDto", clienteDto);
            return "cliente/editarPerfilCliente";
        }

        String email = authentication.getName();
        User user = userService.buscarPorEmail(email).orElse(null);

        if (user != null) {
            user.setUsername(clienteDto.getUsername());
            user.setEmail(clienteDto.getEmail());
            user.setPhone(clienteDto.getPhone());
            user.setAddress(clienteDto.getAddress());

            // actualización de contraseña
            String currentPassword = request.getParameter("currentPassword");
            String newPassword = request.getParameter("newPassword");
            String confirmPassword = request.getParameter("confirmPassword");

            if (currentPassword != null && !currentPassword.isBlank() &&
                    newPassword != null && !newPassword.isBlank() &&
                    confirmPassword != null && !confirmPassword.isBlank()) {

                if (!userService.verificarPassword(currentPassword, user.getPassword())) {
                    model.addAttribute("error", "La contraseña actual es incorrecta.");
                    model.addAttribute("clienteDto", clienteDto);
                    return "cliente/editarPerfilCliente";
                }

                if (!newPassword.equals(confirmPassword)) {
                    model.addAttribute("error", "Las contraseñas nuevas no coinciden.");
                    model.addAttribute("clienteDto", clienteDto);
                    return "cliente/editarPerfilCliente";
                }

                String codificada = userService.codificarPassword(newPassword);
                user.setPassword(codificada);
            }

            userService.guardar(user);
        }

        return "redirect:/cliente/dashboard";
    }
}
