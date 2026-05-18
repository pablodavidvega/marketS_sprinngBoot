package com.example.MarketS.controller.api;

import com.example.MarketS.dto.PerfilResponseDTO;
import com.example.MarketS.dto.RegistroUsuarioDTO;
import com.example.MarketS.model.Rol;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.RolRepository;
import com.example.MarketS.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminApiController {

    private final UserService userService;
    private final RolRepository rolRepository;

    public AdminApiController(UserService userService, RolRepository rolRepository) {
        this.userService = userService;
        this.rolRepository = rolRepository;
    }

    // GET /api/admin/dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(Map.of(
                "totalUsuarios",   userService.contarTodos(),
                "totalClientes",   userService.contarPorRol("ROLE_CLIENTE"),
                "totalVendedores", userService.contarPorRol("ROLE_VENDEDOR"),
                "totalAdmins",     userService.contarPorRol("ROLE_ADMIN")
        ));
    }

    // GET /api/admin/usuarios?tipo=email&valor=x
    @GetMapping("/usuarios")
    public ResponseEntity<List<PerfilResponseDTO>> listarUsuarios(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String valor) {

        List<User> usuarios;

        if (tipo == null || valor == null || valor.isBlank()) {
            usuarios = userService.listarTodos();
        } else {
            usuarios = switch (tipo) {
                case "nombre" -> userService.buscarPorNombre(valor).map(List::of).orElse(List.of());
                case "email"  -> userService.buscarPorEmail(valor).map(List::of).orElse(List.of());
                case "direccion" -> userService.buscarPorDireccion(valor);
                case "rol"    -> userService.listarPorRol("ROLE_" + valor.toUpperCase());
                default       -> userService.listarTodos();
            };
        }

        List<PerfilResponseDTO> respuesta = usuarios.stream()
                .map(PerfilResponseDTO::from)
                .toList();

        return ResponseEntity.ok(respuesta);
    }

    // GET /api/admin/usuarios/{id}
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<PerfilResponseDTO> obtenerUsuario(@PathVariable Long id) {
        return userService.buscarPorId(id)
                .map(PerfilResponseDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/admin/usuarios — crear nuevo admin
    @PostMapping("/usuarios")
    public ResponseEntity<?> crearAdmin(@Valid @RequestBody RegistroUsuarioDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Las contraseñas no coinciden"));
        }

        Rol rolAdmin = rolRepository.findByRolname("ROLE_ADMIN")
                .orElseThrow(() -> new IllegalStateException("Rol ADMIN no configurado"));

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());
        user.setRol(rolAdmin);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        try {
            User creado = userService.registrarUsuario(user);
            return ResponseEntity.ok(PerfilResponseDTO.from(creado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/admin/usuarios/{id}/rol — cambiar rol de un usuario
    @PutMapping("/usuarios/{id}/rol")
    public ResponseEntity<?> cambiarRol(@PathVariable Long id,
                                         @RequestBody Map<String, Long> body) {
        Long rolId = body.get("rolId");
        if (rolId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Se requiere rolId"));
        }

        if (!rolRepository.existsById(rolId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rol no encontrado"));
        }

        userService.cambiarRol(id, rolId);

        return userService.buscarPorId(id)
                .map(PerfilResponseDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/admin/usuarios/{id}
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (userService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        userService.eliminarUsuario(id);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario eliminado correctamente"));
    }

    // GET /api/admin/roles — lista de roles disponibles
    @GetMapping("/roles")
    public ResponseEntity<List<Map<String, Object>>> listarRoles() {
        List<Map<String, Object>> roles = rolRepository.findAll().stream()
                .map(r -> Map.<String, Object>of("id", r.getId(), "nombre", r.getRolname()))
                .toList();
        return ResponseEntity.ok(roles);
    }
}
