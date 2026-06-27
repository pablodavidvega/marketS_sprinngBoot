package com.example.MarketS.controller.api;

import com.example.MarketS.dto.LoginDTO;
import com.example.MarketS.dto.PerfilResponseDTO;
import com.example.MarketS.dto.RegistroVendedorDTO;
import com.example.MarketS.model.Rol;
import com.example.MarketS.model.User;
import com.example.MarketS.model.Vendedor;
import com.example.MarketS.repository.RolRepository;
import com.example.MarketS.security.JwtTokenProvider;
import com.example.MarketS.service.ConfiguracionAppService;
import com.example.MarketS.service.EmailService;
import com.example.MarketS.service.InvitacionService;
import com.example.MarketS.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private static final String COOKIE_NAME = "auth_token";
    private static final Duration COOKIE_MAX_AGE = Duration.ofMinutes(15);

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${cookie.samesite:Lax}")
    private String cookieSameSite;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final RolRepository rolRepository;
    private final EmailService emailService;
    private final ConfiguracionAppService configService;
    private final InvitacionService invitacionService;

    public AuthApiController(AuthenticationManager authenticationManager,
                              JwtTokenProvider tokenProvider,
                              UserService userService,
                              RolRepository rolRepository,
                              EmailService emailService,
                              ConfiguracionAppService configService,
                              InvitacionService invitacionService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
        this.rolRepository = rolRepository;
        this.emailService = emailService;
        this.configService = configService;
        this.invitacionService = invitacionService;
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    // POST /api/auth/login — devuelve JWT como cookie httpOnly (no en el body)
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletResponse response) {
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

            response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(token, COOKIE_MAX_AGE).toString());
            return ResponseEntity.ok(Map.of("email", loginDTO.getEmail(), "role", role));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas"));
        }
    }

    // POST /api/auth/logout — limpia la cookie
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO).toString());
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada"));
    }

    // POST /api/auth/registro
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@Valid @RequestBody RegistroVendedorDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Las contraseñas no coinciden"));
        }

        // ── Validación de acceso según modo de registro ──────────────────────
        String modo    = configService.getModoRegistro();
        String dominio = configService.getDominioPermitido();
        String token   = dto.getInvitacion();

        if (!ConfiguracionAppService.MODO_ABIERTO.equals(modo)) {
            boolean dominioOk    = false;
            boolean invitacionOk = false;

            if ((ConfiguracionAppService.MODO_DOMINIO.equals(modo) ||
                 ConfiguracionAppService.MODO_DOMINIO_O_INVITACION.equals(modo))
                 && !dominio.isBlank()) {
                String emailDomain = dto.getEmail().contains("@")
                        ? dto.getEmail().substring(dto.getEmail().indexOf('@') + 1).toLowerCase()
                        : "";
                dominioOk = emailDomain.equalsIgnoreCase(dominio.trim());
            }

            if ((ConfiguracionAppService.MODO_INVITACION.equals(modo) ||
                 ConfiguracionAppService.MODO_DOMINIO_O_INVITACION.equals(modo))
                 && token != null && !token.isBlank()) {
                invitacionOk = invitacionService.esVigente(token);
            }

            boolean acceso = switch (modo) {
                case ConfiguracionAppService.MODO_DOMINIO             -> dominioOk;
                case ConfiguracionAppService.MODO_INVITACION          -> invitacionOk;
                case ConfiguracionAppService.MODO_DOMINIO_O_INVITACION -> dominioOk || invitacionOk;
                default -> true;
            };

            if (!acceso) {
                String mensaje = switch (modo) {
                    case ConfiguracionAppService.MODO_DOMINIO ->
                            "Solo pueden registrarse usuarios con correo @" + dominio;
                    case ConfiguracionAppService.MODO_INVITACION ->
                            "El registro requiere un link de invitación válido";
                    default ->
                            "Acceso restringido: se requiere correo @" + dominio + " o link de invitación";
                };
                return ResponseEntity.status(403).body(Map.of("error", mensaje));
            }
        }
        // ─────────────────────────────────────────────────────────────────────

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
            // Consumir un uso del token si se usó para el registro
            if (token != null && !token.isBlank()) {
                invitacionService.usarToken(token);
            }
            return ResponseEntity.ok(PerfilResponseDTO.from(creado));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo o nombre de usuario ya está en uso"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al registrar usuario"));
        }
    }

    // POST /api/auth/forgot-password — genera token y envía email con link al frontend React
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo es requerido"));
        }

        var userOpt = userService.buscarPorEmail(email.trim());
        // Respuesta genérica para no revelar si el correo existe o no
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("mensaje", "Si el correo está registrado, recibirás un enlace en breve"));
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpira(LocalDateTime.now().plusMinutes(30));
        userService.guardar(user);

        String link = frontendUrl + "/reset-password?token=" + token;
        emailService.enviarCorreo(
                email,
                "Restablecer contraseña - MarketS",
                "Haz clic en el siguiente enlace para restablecer tu contraseña (válido por 30 minutos):\n\n" + link
        );

        return ResponseEntity.ok(Map.of("mensaje", "Si el correo está registrado, recibirás un enlace en breve"));
    }

    // POST /api/auth/reset-password — valida token y actualiza contraseña
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String password = body.get("password");

        if (token == null || token.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token y contraseña son requeridos"));
        }

        var userOpt = userService.buscarPorToken(token);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token inválido o expirado"));
        }

        User user = userOpt.get();

        if (user.getResetTokenExpira() != null && user.getResetTokenExpira().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El enlace ha expirado. Solicita uno nuevo"));
        }

        user.setPassword(userService.codificarPassword(password));
        user.setResetToken(null);
        user.setResetTokenExpira(null);
        userService.guardar(user);

        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente"));
    }
}
