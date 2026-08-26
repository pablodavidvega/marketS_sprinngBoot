package com.example.MarketS.controller.api;

import com.example.MarketS.dto.LoginDTO;
import com.example.MarketS.dto.PerfilResponseDTO;
import com.example.MarketS.dto.RegistroVendedorDTO;
import com.example.MarketS.exception.RecursoNoEncontradoException;
import com.example.MarketS.model.Rol;
import com.example.MarketS.model.User;
import com.example.MarketS.model.Vendedor;
import com.example.MarketS.repository.RolRepository;
import com.example.MarketS.security.AuthCookieService;
import com.example.MarketS.security.JwtTokenProvider;
import com.example.MarketS.security.RateLimitService;
import com.example.MarketS.security.UsuarioPrincipal;
import com.example.MarketS.service.EmailService;
import com.example.MarketS.service.InvitacionService;
import com.example.MarketS.service.NotificacionService;
import com.example.MarketS.service.RegistroElegibilidadService;
import com.example.MarketS.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
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

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    // Misma vida útil que el JWT (jwt.expiration) — así la cookie nunca vence antes que el token.
    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private Duration cookieMaxAge() {
        return Duration.ofMillis(jwtExpirationMs);
    }

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final RolRepository rolRepository;
    private final EmailService emailService;
    private final NotificacionService notificacionService;
    private final InvitacionService invitacionService;
    private final RegistroElegibilidadService elegibilidadService;
    private final AuthCookieService cookieService;
    private final RateLimitService rateLimitService;

    public AuthApiController(AuthenticationManager authenticationManager,
                              JwtTokenProvider tokenProvider,
                              UserService userService,
                              RolRepository rolRepository,
                              EmailService emailService,
                              NotificacionService notificacionService,
                              InvitacionService invitacionService,
                              RegistroElegibilidadService elegibilidadService,
                              AuthCookieService cookieService,
                              RateLimitService rateLimitService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
        this.rolRepository = rolRepository;
        this.emailService = emailService;
        this.notificacionService = notificacionService;
        this.invitacionService = invitacionService;
        this.elegibilidadService = elegibilidadService;
        this.cookieService = cookieService;
        this.rateLimitService = rateLimitService;
    }

    // POST /api/auth/login — devuelve el JWT tanto en cookie httpOnly (funciona same-site/dev)
    // como en el body (para que el frontend lo mande como Authorization: Bearer cuando frontend
    // y backend están en dominios distintos — Safari/iOS bloquea la cookie cross-site por ITP)
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response) {
        if (!rateLimitService.permitirLogin(request, loginDTO.getEmail())) {
            return ResponseEntity.status(429).body(Map.of("error", "Demasiados intentos. Probá de nuevo en unos minutos."));
        }
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
            User usuario = UsuarioPrincipal.from(auth);

            response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildCookie(token, cookieMaxAge()).toString());
            return ResponseEntity.ok(Map.of(
                    "email", loginDTO.getEmail(),
                    "role", role,
                    "token", token,
                    "debeCambiarPassword", usuario != null && usuario.isDebeCambiarPassword()
            ));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas"));
        }
    }

    // POST /api/auth/refresh — renueva el JWT del usuario autenticado (sliding session).
    // Requiere un token todavía válido (no está en la lista de permitAll de SecurityConfig,
    // así que anyRequest().authenticated() lo protege); el frontend lo llama periódicamente
    // mientras detecta actividad para que la sesión no expire estando en uso.
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(Authentication authentication, HttpServletResponse response) {
        String token = tokenProvider.generateToken(authentication);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildCookie(token, cookieMaxAge()).toString());
        return ResponseEntity.ok(Map.of("token", token));
    }

    // POST /api/auth/logout — limpia la cookie
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildCookie("", Duration.ZERO).toString());
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada"));
    }

    // POST /api/auth/registro
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@Valid @RequestBody RegistroVendedorDTO dto, HttpServletRequest request) {
        if (!rateLimitService.permitirRegistro(request)) {
            return ResponseEntity.status(429).body(Map.of("error", "Demasiados intentos. Probá de nuevo en un rato."));
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Las contraseñas no coinciden"));
        }

        // ── Validación de acceso según modo de registro ──────────────────────
        String token = dto.getInvitacion();
        var evaluacion = elegibilidadService.evaluar(dto.getEmail(), token);
        if (!evaluacion.permitido()) {
            return ResponseEntity.status(403).body(Map.of("error", evaluacion.mensajeError()));
        }
        // ─────────────────────────────────────────────────────────────────────

        if (userService.buscarPorEmail(dto.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo ya está registrado"));
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
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol no configurado: " + tipoRol));

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());
        user.setRol(rol);
        user.setAceptoTerminos(dto.isAceptaTerminos());
        user.setFechaAceptacionTerminos(LocalDateTime.now());

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
            emailService.enviarBienvenida(creado.getEmail(), creado.getUsername(), rol.getRolname());
            if ("VENDEDOR".equals(tipoRol)) {
                emailService.enviarGuiaMercadoPago(creado.getEmail(), creado.getUsername());
                notificacionService.crear(creado,
                        "Ya sos vendedor en MarketS. Configurá tu cuenta de MercadoPago para poder cobrar tus ventas.",
                        "VENDEDOR_CONFIGURAR_MP", null);
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
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo es requerido"));
        }

        if (!rateLimitService.permitirForgotPassword(request, email)) {
            // Misma respuesta genérica que el resto del endpoint, para no revelar que se activó
            // un límite (que a su vez podría filtrar si el correo existe).
            return ResponseEntity.ok(Map.of("mensaje", "Si el correo está registrado, recibirás un enlace en breve"));
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
        user.setPasswordEstablecida(true);
        user.setResetToken(null);
        user.setResetTokenExpira(null);
        userService.guardar(user);

        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente"));
    }
}
