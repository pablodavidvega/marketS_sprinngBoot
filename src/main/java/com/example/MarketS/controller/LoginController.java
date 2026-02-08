package com.example.MarketS.controller;

import com.example.MarketS.dto.LoginDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.MarketS.service.UserService;
import java.time.LocalDateTime;
import java.util.UUID;
import com.example.MarketS.model.User;
import com.example.MarketS.service.EmailService;

@Controller
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;



    @GetMapping("/login")
    public String mostrarFormularioLogin(Model model) {
        model.addAttribute("loginDTO", new LoginDTO());
        return "login";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam String token, Model model) {

        var userOpt = userService.buscarPorToken(token);

        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Token inválido o expirado");
            return "auth/forgot-password";
        }

        model.addAttribute("token", token);
        return "auth/reset-password";
    }


    @PostMapping("/forgot-password")
    public String enviarLinkReset(@RequestParam String email, Model model) {

        var userOpt = userService.buscarPorEmail(email);

        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Correo no registrado");
            return "auth/forgot-password";
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();

        user.setResetToken(token);
        user.setResetTokenExpira(LocalDateTime.now().plusMinutes(30));
        userService.guardar(user);

        emailService.enviarCorreo(
                email,
                "Restablecer contraseña",
                "http://localhost:8081/reset-password?token=" + token
        );

        model.addAttribute("mensaje", "Revisa tu correo");
        return "auth/forgot-password";
    }


    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String token,
            @RequestParam String password,
            Model model) {

        var userOpt = userService.buscarPorToken(token);

        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Token inválido");
            return "auth/reset-password";
        }

        User user = userOpt.get();

        user.setPassword(password); // se codifica en el service
        user.setResetToken(null);
        user.setResetTokenExpira(null);

        userService.guardar(user);

        model.addAttribute("mensaje", "Contraseña actualizada");
        return "login";
    }

}

