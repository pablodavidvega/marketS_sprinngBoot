package com.example.MarketS.controller.api;

import com.example.MarketS.model.Pregunta;
import com.example.MarketS.model.Producto;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.PreguntaRepository;
import com.example.MarketS.repository.ProductoRepository;
import com.example.MarketS.repository.UserRepository;
import com.example.MarketS.security.UsuarioPrincipal;
import com.example.MarketS.service.EmailService;
import com.example.MarketS.service.FrontendLinkBuilder;
import com.example.MarketS.service.NotificacionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos/{productoId}/preguntas")
public class PreguntaApiController {

    private final PreguntaRepository preguntaRepository;
    private final ProductoRepository productoRepository;
    private final NotificacionService notificacionService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final FrontendLinkBuilder frontendLinkBuilder;

    public PreguntaApiController(PreguntaRepository preguntaRepository,
                                  ProductoRepository productoRepository,
                                  NotificacionService notificacionService,
                                  EmailService emailService,
                                  UserRepository userRepository,
                                  FrontendLinkBuilder frontendLinkBuilder) {
        this.preguntaRepository = preguntaRepository;
        this.productoRepository = productoRepository;
        this.notificacionService = notificacionService;
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.frontendLinkBuilder = frontendLinkBuilder;
    }

    // GET /api/productos/{productoId}/preguntas — público, paginado real (usado por DetallePage
    // con su botón "Ver más").
    @GetMapping
    public ResponseEntity<?> listar(@PathVariable Long productoId, Authentication auth,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "30") int size) {
        Producto producto = productoRepository.findById(productoId).orElse(null);
        if (producto == null) return ResponseEntity.notFound().build();

        Page<Pregunta> pagina = preguntaRepository.findByProductoIdOrderByFechaPreguntaDesc(productoId, PageRequest.of(page, size));
        List<Map<String, Object>> preguntas = pagina.getContent()
                .stream()
                .map(this::toMap)
                .toList();

        boolean puedeResponder = false;
        if (auth != null && auth.isAuthenticated()) {
            User usuario = UsuarioPrincipal.from(auth);
            if (usuario != null) {
                boolean esAdmin = "ROLE_ADMIN".equals(usuario.getRol().getRolname());
                boolean esVendedorDelProducto = producto.getVendedor() != null
                        && producto.getVendedor().getId().equals(usuario.getId());
                puedeResponder = esAdmin || esVendedorDelProducto;
            }
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("total", preguntaRepository.countByProductoId(productoId));
        respuesta.put("puedeResponder", puedeResponder);
        respuesta.put("preguntas", preguntas);
        respuesta.put("paginaActual", pagina.getNumber());
        respuesta.put("totalPaginas", pagina.getTotalPages());
        return ResponseEntity.ok(respuesta);
    }

    // POST /api/productos/{productoId}/preguntas — requiere auth, cualquier usuario logueado
    @PostMapping
    public ResponseEntity<?> crear(@PathVariable Long productoId,
                                    @RequestBody Map<String, Object> body,
                                    Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) return ResponseEntity.status(401).build();

        Producto producto = productoRepository.findById(productoId).orElse(null);
        if (producto == null) return ResponseEntity.notFound().build();

        String contenido = body.get("contenido") instanceof String s ? s.trim() : null;
        if (contenido == null || contenido.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La pregunta no puede estar vacía"));
        }

        Pregunta pregunta = new Pregunta();
        pregunta.setProducto(producto);
        pregunta.setUsuario(usuario);
        pregunta.setContenido(contenido);
        pregunta.setFechaPregunta(LocalDateTime.now());
        preguntaRepository.save(pregunta);

        User vendedorRef = producto.getVendedor();
        if (vendedorRef != null) {
            notificacionService.crear(
                    vendedorRef,
                    usuario.getUsername() + " preguntó sobre " + producto.getNombre(),
                    "NUEVA_PREGUNTA",
                    null,
                    productoId
            );
            // Se recarga el vendedor completo aparte: producto.getVendedor() es LAZY y, en este
            // punto, ya quedó fuera de la sesión que lo cargó (cada llamada a un repository
            // corre en su propia transacción), así que solo su id es seguro de leer sin querer.
            userRepository.findById(vendedorRef.getId()).ifPresent(vendedor -> {
                if (vendedor.getEmail() != null && !vendedor.getEmail().isBlank()) {
                    emailService.enviarCorreo(
                            vendedor.getEmail(),
                            "Nueva pregunta sobre " + producto.getNombre() + " - MarketS",
                            usuario.getUsername() + " preguntó:\n\n" + contenido
                                    + "\n\nVer en MarketS: " + frontendLinkBuilder.producto(productoId)
                    );
                }
            });
        }

        return ResponseEntity.ok(toMap(pregunta));
    }

    // PATCH /api/productos/{productoId}/preguntas/{preguntaId}/responder — solo el vendedor del producto o admin
    @PatchMapping("/{preguntaId}/responder")
    public ResponseEntity<?> responder(@PathVariable Long productoId, @PathVariable Long preguntaId,
                                        @RequestBody Map<String, Object> body, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) return ResponseEntity.status(401).build();

        Pregunta pregunta = preguntaRepository.findById(preguntaId).orElse(null);
        if (pregunta == null || !pregunta.getProducto().getId().equals(productoId)) {
            return ResponseEntity.notFound().build();
        }

        // Se vuelve a cargar el producto desde su propio repositorio (en vez de usar
        // pregunta.getProducto(), que es LAZY y ya quedó fuera de sesión en este punto)
        // para evitar LazyInitializationException al acceder a producto.getVendedor().
        Producto producto = productoRepository.findById(productoId).orElse(null);
        if (producto == null) return ResponseEntity.notFound().build();

        boolean esAdmin = "ROLE_ADMIN".equals(usuario.getRol().getRolname());
        boolean esVendedorDelProducto = producto.getVendedor() != null
                && producto.getVendedor().getId().equals(usuario.getId());
        if (!esAdmin && !esVendedorDelProducto) {
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        }

        String respuesta = body.get("respuesta") instanceof String s ? s.trim() : null;
        if (respuesta == null || respuesta.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La respuesta no puede estar vacía"));
        }

        pregunta.setRespuesta(respuesta);
        pregunta.setFechaRespuesta(LocalDateTime.now());
        preguntaRepository.save(pregunta);

        User preguntador = pregunta.getUsuario();
        notificacionService.crear(
                preguntador,
                "Respondieron tu pregunta sobre " + producto.getNombre(),
                "PREGUNTA_RESPONDIDA",
                null,
                productoId
        );
        if (preguntador.getEmail() != null && !preguntador.getEmail().isBlank()) {
            emailService.enviarCorreo(
                    preguntador.getEmail(),
                    "Respondieron tu pregunta sobre " + producto.getNombre() + " - MarketS",
                    "Tu pregunta:\n" + pregunta.getContenido() + "\n\nRespuesta:\n" + respuesta
                            + "\n\nVer en MarketS: " + frontendLinkBuilder.producto(productoId)
            );
        }

        return ResponseEntity.ok(toMap(pregunta));
    }

    private Map<String, Object> toMap(Pregunta p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("contenido", p.getContenido());
        map.put("usuarioUsername", p.getUsuario().getUsername());
        map.put("fechaPregunta", p.getFechaPregunta().toString());
        map.put("respuesta", p.getRespuesta());
        map.put("fechaRespuesta", p.getFechaRespuesta() != null ? p.getFechaRespuesta().toString() : null);
        return map;
    }
}
