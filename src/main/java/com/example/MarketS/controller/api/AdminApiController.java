package com.example.MarketS.controller.api;

import com.example.MarketS.dto.PerfilResponseDTO;
import com.example.MarketS.dto.RegistroUsuarioDTO;
import com.example.MarketS.dto.VendedorEstadisticasDTO;
import com.example.MarketS.model.Rol;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.RolRepository;
import com.example.MarketS.repository.UserRepository;
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
    private final UserRepository userRepository;

    public AdminApiController(UserService userService, RolRepository rolRepository, UserRepository userRepository) {
        this.userService = userService;
        this.rolRepository = rolRepository;
        this.userRepository = userRepository;
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

        User usuario = userService.buscarPorId(id).orElse(null);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        Rol rolNuevo = rolRepository.findById(rolId).orElse(null);
        if (rolNuevo == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rol no encontrado"));
        }

        // ── Regla de negocio: un vendedor NO puede degradarse a cliente ──────
        // Sus productos publicados quedarían sin administrador.
        String rolActual = usuario.getRol() != null ? usuario.getRol().getRolname() : "";
        if ("ROLE_VENDEDOR".equals(rolActual) && "ROLE_CLIENTE".equals(rolNuevo.getRolname())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "No se puede cambiar un vendedor a cliente. " +
                         "El vendedor tiene productos publicados que quedarían sin administrador."
            ));
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

    // GET /api/admin/reportes/vendedores — estadísticas agregadas por vendedor (también accesible por MODERADOR)
    @GetMapping("/reportes/vendedores")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MODERADOR')")
    public ResponseEntity<List<VendedorEstadisticasDTO>> reporteVendedores() {
        List<VendedorEstadisticasDTO> stats = userRepository.findEstadisticasTodosVendedores()
                .stream()
                .map(VendedorEstadisticasDTO::from)
                .toList();
        return ResponseEntity.ok(stats);
    }
}
