package com.example.MarketS.controller.api;

import com.example.MarketS.dto.ActualizarPerfilRequestDTO;
import com.example.MarketS.dto.CambiarPasswordRequestDTO;
import com.example.MarketS.dto.PerfilResponseDTO;
import com.example.MarketS.model.User;
import com.example.MarketS.security.UsuarioPrincipal;
import com.example.MarketS.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class UsuarioApiController {

    private final UserService userService;

    public UsuarioApiController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/me — perfil del usuario autenticado
    @GetMapping
    public ResponseEntity<PerfilResponseDTO> perfil(Authentication auth) {
        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(PerfilResponseDTO.from(usuario));
    }

    // PUT /api/me — actualizar datos básicos del perfil
    @PutMapping
    public ResponseEntity<?> actualizar(@Valid @RequestBody ActualizarPerfilRequestDTO request,
                                         Authentication auth) {
        User user = resolverUsuario(auth);

        // Si el email cambia, verificar que no esté en uso por otro usuario
        if (!user.getEmail().equals(request.getEmail())) {
            boolean emailEnUso = userService.buscarPorEmail(request.getEmail())
                    .map(u -> !u.getId().equals(user.getId()))
                    .orElse(false);
            if (emailEnUso) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El correo ya está registrado por otro usuario"));
            }
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        userService.guardar(user);

        return ResponseEntity.ok(PerfilResponseDTO.from(user));
    }

    // PUT /api/me/password — cambiar contraseña
    @PutMapping("/password")
    public ResponseEntity<?> cambiarPassword(@Valid @RequestBody CambiarPasswordRequestDTO request,
                                              Authentication auth) {
        User user = resolverUsuario(auth);

        if (!userService.verificarPassword(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La contraseña actual es incorrecta"));
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Las contraseñas nuevas no coinciden"));
        }

        if (userService.verificarPassword(request.getNewPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La nueva contraseña no puede ser igual a la actual"));
        }

        user.setPassword(userService.codificarPassword(request.getNewPassword()));
        userService.guardar(user);

        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente"));
    }

    private User resolverUsuario(Authentication auth) {
        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) throw new RuntimeException("Usuario no encontrado");
        return usuario;
    }
}
