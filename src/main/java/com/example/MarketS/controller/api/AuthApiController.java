package com.example.MarketS.controller.api;

import com.example.MarketS.dto.AuthResponseDTO;
import com.example.MarketS.dto.LoginDTO;
import com.example.MarketS.dto.PerfilResponseDTO;
import com.example.MarketS.dto.RegistroVendedorDTO;
import com.example.MarketS.model.Rol;
import com.example.MarketS.model.User;
import com.example.MarketS.model.Vendedor;
import com.example.MarketS.repository.RolRepository;
import com.example.MarketS.security.JwtTokenProvider;
import com.example.MarketS.service.UserService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final RolRepository rolRepository;

    public AuthApiController(AuthenticationManager authenticationManager,
                              JwtTokenProvider tokenProvider,
                              UserService userService,
                              RolRepository rolRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
        this.rolRepository = rolRepository;
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginDTO) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword())
            );
            String token = tokenProvider.generateToken(auth);
            String role = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("")
                    .replace("ROLE_", "");
            return ResponseEntity.ok(new AuthResponseDTO(token, loginDTO.getEmail(), role));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas"));
        }
    }

    // POST /api/auth/registro
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@Valid @RequestBody RegistroVendedorDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Las contraseñas no coinciden"));
        }

        String tipoRol = (dto.getTipoUsuario() == null || dto.getTipoUsuario().isBlank())
                ? "CLIENTE"
                : dto.getTipoUsuario().toUpperCase();

        if (!tipoRol.equals("CLIENTE") && !tipoRol.equals("VENDEDOR")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tipo de usuario no válido"));
        }

        if ("VENDEDOR".equals(tipoRol) && (dto.getCity() == null || dto.getCity().isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "La ciudad es obligatoria para vendedores"));
        }

        Rol rol = rolRepository.findByRolname("ROLE_" + tipoRol)
                .orElseThrow(() -> new IllegalStateException("Rol no configurado: " + tipoRol));

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());
        user.setRol(rol);

        if ("VENDEDOR".equals(tipoRol)) {
            Vendedor vendedor = new Vendedor();
            vendedor.setCity(dto.getCity());
            vendedor.setUser(user);
            user.setVendedor(vendedor);
        }

        try {
            User creado = userService.registrarUsuario(user);
            return ResponseEntity.ok(PerfilResponseDTO.from(creado));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo o nombre de usuario ya está en uso"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al registrar usuario"));
        }
    }
}
