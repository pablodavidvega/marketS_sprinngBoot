package com.example.MarketS.controller.api;

import com.example.MarketS.dto.GoogleCompletarRegistroDTO;
import com.example.MarketS.dto.GoogleLoginRequestDTO;
import com.example.MarketS.model.Rol;
import com.example.MarketS.model.User;
import com.example.MarketS.model.Vendedor;
import com.example.MarketS.repository.RolRepository;
import com.example.MarketS.repository.UserRepository;
import com.example.MarketS.security.AuthCookieService;
import com.example.MarketS.security.GoogleTokenVerifierService;
import com.example.MarketS.security.JwtTokenProvider;
import com.example.MarketS.service.EmailService;
import com.example.MarketS.service.InvitacionService;
import com.example.MarketS.service.NotificacionService;
import com.example.MarketS.service.RegistroElegibilidadService;
import com.example.MarketS.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Login/registro con Google (Google Identity Services): el frontend manda el id_token que
// Google le dio al usuario, este controller lo verifica y, si es válido, emite la misma
// cookie httpOnly "auth_token" que emite el login clásico — el resto del sistema
// (JwtAuthenticationFilter, SecurityConfig, AuthContext del frontend) no necesita saber que
// el origen fue Google.
@RestController
@RequestMapping("/api/auth/google")
public class GoogleAuthApiController {

    // Misma vida útil que el JWT (jwt.expiration) — así la cookie nunca vence antes que el token.
    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private Duration cookieMaxAge() {
        return Duration.ofMillis(jwtExpirationMs);
    }

    @Value("${google.oauth.pending-token.expiration:600000}")
    private long pendingTokenTtlMillis;

    private final GoogleTokenVerifierService googleTokenVerifier;
    private final JwtTokenProvider tokenProvider;
    private final AuthCookieService cookieService;
    private final UserRepository userRepository;
    private final RolRepository rolRepository;
    private final UserService userService;
    private final RegistroElegibilidadService elegibilidadService;
    private final InvitacionService invitacionService;
    private final EmailService emailService;
    private final NotificacionService notificacionService;

    public GoogleAuthApiController(GoogleTokenVerifierService googleTokenVerifier,
                                    JwtTokenProvider tokenProvider,
                                    AuthCookieService cookieService,
                                    UserRepository userRepository,
                                    RolRepository rolRepository,
                                    UserService userService,
                                    RegistroElegibilidadService elegibilidadService,
                                    InvitacionService invitacionService,
                                    EmailService emailService,
                                    NotificacionService notificacionService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.tokenProvider = tokenProvider;
        this.cookieService = cookieService;
        this.userRepository = userRepository;
        this.rolRepository = rolRepository;
        this.userService = userService;
        this.elegibilidadService = elegibilidadService;
        this.invitacionService = invitacionService;
        this.emailService = emailService;
        this.notificacionService = notificacionService;
    }

    // POST /api/auth/google — login directo si el usuario ya existe (por googleId o por
    // email, vinculando en ese caso), o arranca el alta pendiente si es un email nuevo.
    @PostMapping
    public ResponseEntity<?> loginOrStart(@Valid @RequestBody GoogleLoginRequestDTO dto, HttpServletResponse response) {
        GoogleTokenVerifierService.GooglePayload payload;
        try {
            payload = googleTokenVerifier.verify(dto.getIdToken());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token de Google inválido o expirado"));
        }

        if (!payload.emailVerified()) {
            return ResponseEntity.status(403).body(Map.of("error", "Tu correo de Google no está verificado"));
        }

        Optional<User> porGoogleId = userRepository.findByGoogleId(payload.googleId());
        if (porGoogleId.isPresent()) {
            return emitirCookieYResponder(porGoogleId.get(), response);
        }

        Optional<User> porEmail = userRepository.findByEmail(payload.email());
        if (porEmail.isPresent()) {
            User user = porEmail.get();
            user.setGoogleId(payload.googleId());
            userRepository.save(user);
            return emitirCookieYResponder(user, response);
        }

        // Alta nueva: todavía no hay código de invitación, solo se puede evaluar dominio/modo.
        var evaluacion = elegibilidadService.evaluar(payload.email(), null);
        if (evaluacion.resultado() == RegistroElegibilidadService.Resultado.DENEGADO) {
            return ResponseEntity.status(403).body(Map.of("error", evaluacion.mensajeError()));
        }

        String pendingToken = tokenProvider.generatePendingSignupToken(
                payload.email(), payload.googleId(), payload.name(), pendingTokenTtlMillis);

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("needsCompletion", true);
        body.put("email", payload.email());
        body.put("nombreSugerido", payload.name() == null ? "" : payload.name());
        body.put("pendingToken", pendingToken);
        body.put("requiereInvitacion", evaluacion.resultado() == RegistroElegibilidadService.Resultado.REQUIERE_INVITACION);
        return ResponseEntity.ok(body);
    }

    // POST /api/auth/google/completar — crea la cuenta con los datos que Google no provee
    // (rol, teléfono, dirección, ciudad si vendedor) y emite la sesión.
    @PostMapping("/completar")
    public ResponseEntity<?> completar(@Valid @RequestBody GoogleCompletarRegistroDTO dto, HttpServletResponse response) {
        Claims claims;
        try {
            claims = tokenProvider.parsePendingSignupToken(dto.getPendingToken());
        } catch (JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", "El enlace de registro expiró, volvé a intentar con Google"));
        }
        String email = claims.getSubject();
        String googleId = claims.get("googleId", String.class);

        if (userRepository.findByEmail(email).isPresent() || userRepository.findByGoogleId(googleId).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Esta cuenta ya fue creada, iniciá sesión con Google directamente"));
        }

        var evaluacion = elegibilidadService.evaluar(email, dto.getInvitacion());
        if (!evaluacion.permitido()) {
            return ResponseEntity.status(403).body(Map.of("error", evaluacion.mensajeError()));
        }

        String tipoRol = (dto.getTipoUsuario() == null || dto.getTipoUsuario().isBlank())
                ? "" : dto.getTipoUsuario().toUpperCase();
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
        user.setEmail(email);
        // Password aleatoria que el usuario nunca conoce: esta cuenta solo se usa vía Google.
        // Evita tener que tolerar password null en CustomUserDetailsService/UsuarioPrincipal.
        // passwordEstablecida=false marca que no es una password real; el usuario puede crear
        // una desde su perfil (UsuarioApiController.cambiarPassword) o vía "olvidé mi
        // contraseña", cualquiera de los dos la deja en true.
        user.setPassword(UUID.randomUUID().toString() + UUID.randomUUID());
        user.setPasswordEstablecida(false);
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());
        user.setRol(rol);
        user.setGoogleId(googleId);
        user.setAceptoTerminos(dto.isAceptaTerminos());
        user.setFechaAceptacionTerminos(LocalDateTime.now());

        if ("VENDEDOR".equals(tipoRol)) {
            Vendedor vendedor = new Vendedor();
            vendedor.setCity(dto.getCity());
            vendedor.setUser(user);
            user.setVendedor(vendedor);
        }

        User creado;
        try {
            creado = userService.registrarUsuario(user);
            if (dto.getInvitacion() != null && !dto.getInvitacion().isBlank()) {
                invitacionService.usarToken(dto.getInvitacion());
            }
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "El correo o nombre de usuario ya está en uso"));
        }

        emailService.enviarBienvenida(creado.getEmail(), creado.getUsername(), rol.getRolname());
        if ("VENDEDOR".equals(tipoRol)) {
            emailService.enviarGuiaMercadoPago(creado.getEmail(), creado.getUsername());
            notificacionService.crear(creado,
                    "Ya sos vendedor en MarketS. Configurá tu cuenta de MercadoPago para poder cobrar tus ventas.",
                    "VENDEDOR_CONFIGURAR_MP", null);
        }
        return emitirCookieYResponder(creado, response);
    }

    private ResponseEntity<?> emitirCookieYResponder(User user, HttpServletResponse response) {
        Authentication authStub = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null, List.of(new SimpleGrantedAuthority(user.getRol().getRolname())));
        String token = tokenProvider.generateToken(authStub);
        String role = user.getRol().getRolname().replace("ROLE_", "");

        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.buildCookie(token, cookieMaxAge()).toString());
        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "role", role,
                "needsCompletion", false,
                "token", token
        ));
    }
}
