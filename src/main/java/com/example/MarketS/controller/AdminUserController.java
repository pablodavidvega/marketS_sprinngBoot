package com.example.MarketS.controller;

import com.example.MarketS.dto.RegistroUsuarioDTO;
import com.example.MarketS.model.User;
import com.example.MarketS.model.Rol;
import com.example.MarketS.service.ReportePdfService;
import com.example.MarketS.service.UserService;
import com.example.MarketS.repository.RolRepository;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/usuarios")
public class AdminUserController {

    private final UserService userService;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AdminUserController(UserService userService, RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;

    }

    private void agregarAdminAlModelo(Authentication authentication, Model model) {
        String email = authentication.getName();
        User admin = userService.buscarPorEmail(email).orElse(null);
        model.addAttribute("admin", admin);
    }

    // Listar todos los usuarios
    @GetMapping
    public String listarUsuarios(Authentication authentication, Model model) {
        agregarAdminAlModelo(authentication, model);
        List<User> usuarios = userService.listarTodos();
        model.addAttribute("usuarios", usuarios);
        return "admin/lista_usuarios";
    }
    //Filtrar usuarios//
    @GetMapping("/filtrar")
    public String filtrarUsuario(@RequestParam String tipo,
                                 @RequestParam String valor,
                                 Authentication authentication,
                                 Model model) {
        agregarAdminAlModelo(authentication, model);
        List<User> usuarios;

        switch (tipo) {
            case "nombre":
                Optional<User> usuarioPorNombre = userService.buscarPorNombre(valor);
                usuarios = usuarioPorNombre.map(List::of).orElse(List.of());
                break;
            case "email":
                Optional<User> usuarioPorEmail = userService.buscarPorEmail(valor);
                usuarios = usuarioPorEmail.map(List::of).orElse(List.of());
                break;
            case "direcion":
                usuarios = userService.buscarPorDireccion(valor);
                break;
            case "rol":
                usuarios = userService.listarPorRol("ROLE_" + valor.toUpperCase());
                break;
            case "todos":
            default:
                usuarios = userService.listarTodos();
        }

        model.addAttribute("usuarios", usuarios);
        if (usuarios.isEmpty()) {
            model.addAttribute("mensaje", "No se encontraron usuarios con esos datos.");
        }
        return "admin/lista_usuarios";

    }

    // Mostrar formulario para agregar nuevo admin
    @GetMapping("/nuevo-admin")
    public String mostrarFormularioNuevoAdmin(Authentication authentication, Model model) {
        agregarAdminAlModelo(authentication, model);
        model.addAttribute("usuario", new RegistroUsuarioDTO());
        return "admin/form_nuevo_admin";
    }

    // Procesar creación de nuevo admin
    @PostMapping("/nuevo-admin")
    public String crearNuevoAdmin(@Valid @ModelAttribute("usuario") RegistroUsuarioDTO userDto,
                                  BindingResult result,
                                  Authentication authentication,
                                  Model model) {
        agregarAdminAlModelo(authentication, model);
        if (result.hasErrors()) {

            return "admin/form_nuevo_admin";
        }
        if(!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            model.addAttribute("error","Las contraseñas no coinciden");
            return "admin/form_nuevo_admin";
        }
        try {
            Rol adminrol = rolRepository.findByRolname("ROLE_ADMIN")
                    .orElseThrow(() -> new IllegalStateException("Rol ADMIN no configurado"));

            //Crear usuario desde DTO//
            User user = new User();
            user.setUsername(userDto.getUsername());
            user.setEmail(userDto.getEmail());
            user.setPassword(userDto.getPassword());
            user.setPhone(userDto.getPhone());
            user.setAddress(userDto.getAddress());
            user.setRol(adminrol);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            userService.registrarUsuario(user);
            return "redirect:/admin/usuarios";
        } catch (Exception e) {
            model.addAttribute("error", "No se puede crear el nuevo administrador: " + e.getMessage());
            return "admin/form_nuevo_admin";
        }
    }
    @Autowired
    private ReportePdfService reportePdfService;

    @GetMapping("/reporte-pdf")
    public void descargarReporteUsuarios(Authentication authentication, HttpServletResponse response) {
        try {
            List<User> usuarios = userService.listarTodos();
            byte[] pdfBytes = reportePdfService.generarReporteUsuarios(usuarios);

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=reporte_usuarios.pdf");
            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

