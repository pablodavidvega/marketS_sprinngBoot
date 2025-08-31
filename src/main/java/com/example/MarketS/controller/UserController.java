package com.example.MarketS.controller;

import com.example.MarketS.model.User;
import com.example.MarketS.model.Rol;
import com.example.MarketS.model.Vendedor;
import com.example.MarketS.service.UserService;
import com.example.MarketS.dto.RegistroVendedorDTO;
import com.example.MarketS.dto.VistaUsuarioDTO;
import com.example.MarketS.repository.RolRepository;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Arrays;

@Controller
@RequestMapping("/usuarios")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final RolRepository rolRepository;
    private final UserService userService;

    public UserController(RolRepository rolRepository,
                          UserService userService) {
        this.rolRepository = rolRepository;
        this.userService = userService;
    }

    // Mostrar formulario de registro
    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("registroVendedorDTO", new RegistroVendedorDTO());
        return "register";
    }

    // Procesar registro
    @PostMapping("/registro")
    public String registrarUsuario(
            @Valid @ModelAttribute("registroVendedorDTO") RegistroVendedorDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        logger.info("Iniciando registro para email: {}", dto.getEmail());

        if (result.hasErrors()) {
            logger.warn("Errores de validación: {}", result.getAllErrors());
            return "register";
        }

        // Validación manual: que las contraseñas coincidan
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.confirmPassword", "Las contraseñas no coinciden");
            return "register";
        }

        // Validación manual: si es vendedor, city no puede estar vacío
        if ("VENDEDOR".equalsIgnoreCase(dto.getTipoUsuario()) &&
                (dto.getCity() == null || dto.getCity().isBlank())) {
            result.rejectValue("city", "NotBlank", "La ciudad es obligatoria para vendedores");
            return "register";
        }

        try {
            String rolElegido = validarYNormalizarRol(dto.getTipoUsuario(), result);
            if (rolElegido == null) return "register";

            User nuevoUsuario = crearUsuarioDesdeDTO(dto, rolElegido);
            userService.registrarUsuario(nuevoUsuario);
            logger.info("Usuario registrado correctamente: {}", nuevoUsuario.getEmail());

            redirectAttributes.addFlashAttribute("success", "¡Registro exitoso! Ahora puedes iniciar sesión.");
            return "redirect:/login";

        } catch (DataIntegrityViolationException e) {
            logger.error("El correo o usuario ya existe: {}", e.getMessage());
            model.addAttribute("error", "El correo electrónico o nombre de usuario ya está en uso.");
            return "register";
        } catch (IllegalStateException e) {
            logger.error("Error de rol: {}", e.getMessage());
            model.addAttribute("error", "Error en el sistema de roles. Contacta al administrador.");
            return "register";
        } catch (Exception e) {
            logger.error("Error inesperado: {}", e.getMessage(), e);
            model.addAttribute("error", "Error inesperado. Intenta nuevamente.");
            return "register";
        }
    }

    private String validarYNormalizarRol(String rolInput, BindingResult result) {
        if (rolInput == null || rolInput.trim().isEmpty()) {
            return "CLIENTE";
        }

        String rolNormalizado = rolInput.toUpperCase();
        if (!Arrays.asList("CLIENTE", "VENDEDOR").contains(rolNormalizado)) {
            result.rejectValue("tipoUsuario", "error.tipoUsuario", "Rol no válido");
            return null;
        }
        return rolNormalizado;
    }

    private User crearUsuarioDesdeDTO(RegistroVendedorDTO dto, String rolElegido) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());

        Rol rol = rolRepository.findByRolname("ROLE_" + rolElegido)
                .orElseThrow(() -> new IllegalStateException("Rol no configurado: " + rolElegido));
        user.setRol(rol);

        if ("VENDEDOR".equals(rolElegido)) {
            Vendedor vendedor = new Vendedor();
            vendedor.setCity(dto.getCity());
            vendedor.setUser(user);
            user.setVendedor(vendedor);
        }

        return user;
    }

    @GetMapping("/{id}")
    public String verPerfilUsuario(@PathVariable Long id, Model model, Principal principal) {
        logger.info("Solicitando perfil para usuario ID: {}", id);

        try {
            User usuario = userService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean esPropietario = principal.getName().equals(usuario.getEmail());
            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

            if (!esPropietario && !esAdmin) {
                logger.warn("Acceso no autorizado al perfil de usuario {} por {}", usuario.getId(), principal.getName());
                throw new RuntimeException("Acceso no autorizado");
            }

            VistaUsuarioDTO vistaDto = new VistaUsuarioDTO();
            vistaDto.setId(usuario.getId());
            vistaDto.setName(usuario.getUsername());
            vistaDto.setEmail(usuario.getEmail());
            vistaDto.setPhone(usuario.getPhone());
            vistaDto.setAddress(usuario.getAddress());
            vistaDto.setRol(usuario.getRol().getRolname());

            if (usuario.getVendedor() != null) {
                vistaDto.setCity(usuario.getVendedor().getCity());
            }

            model.addAttribute("usuario", vistaDto);
            return "perfil_usuario";

        } catch (Exception e) {
            logger.error("Error al mostrar perfil de usuario: {}", e.getMessage(), e);
            model.addAttribute("error", "No se pudo cargar el perfil del usuario");
            return "error";
        }
    }
    @PostMapping("/actualizar-perfil")
    public String actualizarPerfil(@ModelAttribute VistaUsuarioDTO usuarioDto,
                                   @RequestParam(required = false) String currentPassword,
                                   @RequestParam(required = false) String newPassword,
                                   @RequestParam(required = false) String confirmPassword,
                                   Principal principal,
                                   RedirectAttributes redirect) {
        User user = userService.buscarPorEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setUsername(usuarioDto.getName());
        user.setPhone(usuarioDto.getPhone());
        user.setAddress(usuarioDto.getAddress());

        // Cambio de contraseña si se ingresaron los campos
        if (newPassword != null && !newPassword.isBlank()) {
            if (!userService.verificarPassword(currentPassword, user.getPassword())) {
                redirect.addFlashAttribute("error", "La contraseña actual es incorrecta.");
                return "redirect:/usuarios/editar-perfil";
            }
            if (!newPassword.equals(confirmPassword)) {
                redirect.addFlashAttribute("error", "Las contraseñas nuevas no coinciden.");
                return "redirect:/usuarios/editar-perfil";
            }

            user.setPassword(userService.codificarPassword(newPassword));
        }

        userService.actualizar(user);
        redirect.addFlashAttribute("success", "Perfil actualizado correctamente.");
        return "redirect:/usuarios/dashboard";
    }

}
