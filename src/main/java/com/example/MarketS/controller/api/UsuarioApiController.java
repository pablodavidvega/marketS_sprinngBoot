package com.example.MarketS.controller.api;

import com.example.MarketS.dto.ActualizarEnlacesRequestDTO;
import com.example.MarketS.dto.ActualizarPerfilRequestDTO;
import com.example.MarketS.dto.CambiarPasswordRequestDTO;
import com.example.MarketS.dto.PerfilResponseDTO;
import com.example.MarketS.model.EnlaceSocial;
import com.example.MarketS.model.User;
import com.example.MarketS.model.Vendedor;
import com.example.MarketS.repository.EnlaceSocialRepository;
import com.example.MarketS.repository.VendedorRepository;
import com.example.MarketS.security.UsuarioPrincipal;
import com.example.MarketS.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
public class UsuarioApiController {

    private final UserService userService;
    private final EnlaceSocialRepository enlaceSocialRepository;
    private final VendedorRepository vendedorRepository;

    public UsuarioApiController(UserService userService, EnlaceSocialRepository enlaceSocialRepository,
                                 VendedorRepository vendedorRepository) {
        this.userService = userService;
        this.enlaceSocialRepository = enlaceSocialRepository;
        this.vendedorRepository = vendedorRepository;
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

    // GET /api/me/enlaces — enlaces (redes sociales, sitio web, etc.) del vendedor autenticado.
    // Vacío si el usuario no es vendedor, sin error — es un sub-recurso opcional del perfil.
    @GetMapping("/enlaces")
    public ResponseEntity<List<Map<String, String>>> misEnlaces(Authentication auth) {
        User user = resolverUsuario(auth);
        Vendedor vendedor = user.getVendedor();
        if (vendedor == null) return ResponseEntity.ok(List.of());

        List<Map<String, String>> enlaces = enlaceSocialRepository.findByVendedorId(vendedor.getId()).stream()
                .map(e -> Map.of("etiqueta", e.getEtiqueta(), "url", e.getUrl()))
                .toList();
        return ResponseEntity.ok(enlaces);
    }

    // PUT /api/me/enlaces — reemplaza toda la lista de enlaces del vendedor autenticado.
    // @Transactional porque el User del Authentication viene de una sesión de Hibernate ya
    // cerrada (open-in-view=false, ver application.properties): hay que releer el Vendedor
    // fresco acá para que vendedor.getEnlaces() (colección @OneToMany LAZY) sea accesible.
    @Transactional
    @PutMapping("/enlaces")
    public ResponseEntity<?> actualizarEnlaces(@Valid @RequestBody ActualizarEnlacesRequestDTO request,
                                                Authentication auth) {
        User user = resolverUsuario(auth);
        if (user.getVendedor() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo los vendedores pueden agregar enlaces"));
        }
        Vendedor vendedor = vendedorRepository.findById(user.getVendedor().getId())
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));

        vendedor.getEnlaces().clear();
        List<EnlaceSocial> nuevos = new ArrayList<>();
        if (request.getEnlaces() != null) {
            request.getEnlaces().forEach(item -> {
                EnlaceSocial enlace = new EnlaceSocial();
                enlace.setEtiqueta(item.getEtiqueta().trim());
                enlace.setUrl(item.getUrl().trim());
                enlace.setVendedor(vendedor);
                nuevos.add(enlace);
            });
        }
        vendedor.getEnlaces().addAll(nuevos);
        vendedorRepository.save(vendedor);

        List<Map<String, String>> respuesta = vendedor.getEnlaces().stream()
                .map(e -> Map.of("etiqueta", e.getEtiqueta(), "url", e.getUrl()))
                .toList();
        return ResponseEntity.ok(respuesta);
    }

    private User resolverUsuario(Authentication auth) {
        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) throw new RuntimeException("Usuario no encontrado");
        return usuario;
    }
}
