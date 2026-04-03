package com.example.MarketS.controller;

import com.example.MarketS.model.Contacto;
import com.example.MarketS.model.EstadoContacto;
import com.example.MarketS.model.User;
import com.example.MarketS.service.ContactoService;
import com.example.MarketS.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/servicio-cliente")
public class ServicioClienteController {

    @Autowired
    private ContactoService contactoService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String mostrar(Model model, Authentication auth) {
        model.addAttribute("contacto", new Contacto());

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            User user = userService.buscarPorEmail(auth.getName()).orElse(null);
            if (user != null) {
                model.addAttribute("nombreUsuario", user.getUsername());
                model.addAttribute("rolUsuario", user.getRol().getRolname());
                model.addAttribute("solicitudes", contactoService.listarPorUsuario(user));
            }
        }
        return "servicio-cliente";
    }

    @PostMapping("/enviar")
    public String enviar(@ModelAttribute Contacto contacto, Authentication auth,
                         RedirectAttributes redirectAttributes) {
        contacto.setEstado(EstadoContacto.PENDIENTE);
        contacto.setFechaCreacion(LocalDateTime.now());

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            User user = userService.buscarPorEmail(auth.getName()).orElse(null);
            contacto.setUser(user);
        }

        contactoService.guardar(contacto);
        redirectAttributes.addFlashAttribute("mensajeExito",
                "Tu solicitud fue enviada exitosamente. Te responderemos a la brevedad.");
        return "redirect:/servicio-cliente";
    }
}
