package com.example.MarketS.controller.api;

import com.example.MarketS.exception.NoAutorizadoException;
import com.example.MarketS.exception.RecursoNoEncontradoException;
import com.example.MarketS.exception.SolicitudInvalidaException;
import com.example.MarketS.model.Encuentro;
import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.ResolucionVentaExterna;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.EncuentroRepository;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.repository.ResolucionVentaExternaRepository;
import com.example.MarketS.security.UsuarioPrincipal;
import com.example.MarketS.service.EmailService;
import com.example.MarketS.service.FrontendLinkBuilder;
import com.example.MarketS.service.NotificacionService;
import com.example.MarketS.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pedidos/{pedidoId}/encuentro/{vendedorId}")
public class EncuentroApiController {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EncuentroRepository encuentroRepository;
    private final PedidoRepository pedidoRepository;
    private final ResolucionVentaExternaRepository resolucionVentaExternaRepository;
    private final UserService userService;
    private final NotificacionService notificacionService;
    private final EmailService emailService;
    private final FrontendLinkBuilder frontendLinkBuilder;

    public EncuentroApiController(EncuentroRepository encuentroRepository,
                                   PedidoRepository pedidoRepository,
                                   ResolucionVentaExternaRepository resolucionVentaExternaRepository,
                                   UserService userService,
                                   NotificacionService notificacionService,
                                   EmailService emailService,
                                   FrontendLinkBuilder frontendLinkBuilder) {
        this.encuentroRepository = encuentroRepository;
        this.pedidoRepository = pedidoRepository;
        this.resolucionVentaExternaRepository = resolucionVentaExternaRepository;
        this.userService = userService;
        this.notificacionService = notificacionService;
        this.emailService = emailService;
        this.frontendLinkBuilder = frontendLinkBuilder;
    }

    // GET /api/pedidos/{pedidoId}/encuentro/{vendedorId} — encuentro acordado (mapa vacío si no hay ninguno)
    @GetMapping
    public ResponseEntity<?> obtener(@PathVariable Long pedidoId, @PathVariable Long vendedorId, Authentication auth) {
        User usuario = resolverUsuario(auth);
        Pedido pedido = pedidoRepository.findByIdWithDetalles(pedidoId).orElse(null);
        if (pedido == null) throw new RecursoNoEncontradoException("Pedido no encontrado");
        if (!autorizado(pedido, vendedorId, usuario)) {
            throw new NoAutorizadoException("No autorizado");
        }

        return encuentroRepository.findByPedidoIdAndVendedorId(pedidoId, vendedorId)
                .<ResponseEntity<?>>map(e -> ResponseEntity.ok(toMap(e, usuario, pedidoId, vendedorId)))
                .orElse(ResponseEntity.ok(Map.of()));
    }

    // PUT /api/pedidos/{pedidoId}/encuentro/{vendedorId} — propone o edita el encuentro (upsert)
    @PutMapping
    public ResponseEntity<?> guardar(@PathVariable Long pedidoId, @PathVariable Long vendedorId,
                                      @RequestBody Map<String, Object> body, Authentication auth) {
        User usuario = resolverUsuario(auth);
        Pedido pedido = pedidoRepository.findByIdWithDetalles(pedidoId).orElse(null);
        if (pedido == null) throw new RecursoNoEncontradoException("Pedido no encontrado");
        if (!autorizado(pedido, vendedorId, usuario)) {
            throw new NoAutorizadoException("No autorizado");
        }

        String lugar = body.get("lugar") instanceof String s ? s.trim() : null;
        String fechaHoraStr = body.get("fechaHora") instanceof String s ? s.trim() : null;
        String nota = body.get("nota") instanceof String s ? s.trim() : null;

        if (lugar == null || lugar.isEmpty()) {
            throw new SolicitudInvalidaException("El lugar es obligatorio");
        }
        if (fechaHoraStr == null || fechaHoraStr.isEmpty()) {
            throw new SolicitudInvalidaException("La fecha y hora son obligatorias");
        }

        LocalDateTime fechaHora;
        try {
            fechaHora = LocalDateTime.parse(fechaHoraStr);
        } catch (Exception e) {
            throw new SolicitudInvalidaException("Fecha y hora inválidas");
        }
        if (fechaHora.isBefore(LocalDateTime.now())) {
            throw new SolicitudInvalidaException("La fecha del encuentro debe ser futura");
        }

        User vendedor = vendedorId.equals(usuario.getId()) ? usuario : userService.buscarPorId(vendedorId).orElse(null);
        if (vendedor == null) throw new RecursoNoEncontradoException("Vendedor no encontrado");

        Optional<Encuentro> existente = encuentroRepository.findByPedidoIdAndVendedorId(pedidoId, vendedorId);
        boolean esNuevo = existente.isEmpty();
        Encuentro encuentro = existente.orElseGet(Encuentro::new);

        encuentro.setPedido(pedido);
        encuentro.setVendedor(vendedor);
        encuentro.setPropuestoPor(usuario);
        encuentro.setLugar(lugar);
        encuentro.setFechaHora(fechaHora);
        encuentro.setNota(nota != null && !nota.isEmpty() ? nota : null);
        // Toda propuesta o edición vuelve a dejar el encuentro en PENDIENTE: cambiaron los
        // términos (o es nuevo), así que la contraparte tiene que volver a confirmar.
        encuentro.setEstado("PENDIENTE");
        if (esNuevo) encuentro.setFechaCreacion(LocalDateTime.now());
        encuentro.setFechaActualizacion(LocalDateTime.now());
        encuentroRepository.save(encuentro);

        String mensaje = esNuevo
                ? usuario.getUsername() + " propuso un encuentro para el pedido #" + pedidoId + ": " + lugar + " el " + fechaHora.format(FORMATO_FECHA)
                : usuario.getUsername() + " actualizó el encuentro del pedido #" + pedidoId + ": " + lugar + " el " + fechaHora.format(FORMATO_FECHA);
        notificarOtraParte(pedido, vendedorId, usuario, mensaje,
                esNuevo ? "ENCUENTRO_PROPUESTO" : "ENCUENTRO_ACTUALIZADO", pedidoId);

        if (esNuevo) {
            avisarConfirmarVentaExternaSiAplica(pedido, vendedor);
        }

        return ResponseEntity.ok(toMap(encuentro, usuario, pedidoId, vendedorId));
    }

    // Cuando se agenda el primer encuentro de un pedido pagado "por fuera de la app"
    // (efectivo/transferencia/etc.), le recordamos al vendedor que además de coordinar la
    // entrega tiene que marcar la venta como realizada en su panel: el stock recién se
    // descuenta ahí (PedidoApiController#resolverAcuerdoExterno), no al crear el pedido.
    // Reusa el mismo tipo que el recordatorio diario (RecordatorioPendienteService) para que
    // el frontend no necesite una ruta nueva y para que el cron de las 9am no duplique el
    // aviso el mismo día.
    private void avisarConfirmarVentaExternaSiAplica(Pedido pedido, User vendedor) {
        if (!"ACORDADO_EXTERNO".equals(pedido.getEstado())) return;
        if (resolucionVentaExternaRepository.existsByPedidoIdAndVendedorId(pedido.getId(), vendedor.getId())) return;

        String mensaje = "Ya coordinaste el encuentro del pedido #" + pedido.getId()
                + ": no olvides marcar la venta como realizada en tu panel una vez la concretes, para que se descuente del stock.";
        notificacionService.crear(vendedor, mensaje, "RECORDATORIO_COMPRA_EXTERNA_SIN_CONFIRMAR", pedido.getId());

        if (vendedor.getEmail() != null && !vendedor.getEmail().isBlank()) {
            String link = frontendLinkBuilder.pedido(true, pedido.getId());
            emailService.enviarCorreo(vendedor.getEmail(), "Recordá confirmar la venta del pedido #" + pedido.getId() + " - MarketS",
                    mensaje + "\n\nVer en MarketS: " + link);
        }
    }

    // POST /api/pedidos/{pedidoId}/encuentro/{vendedorId}/confirmar — la contraparte acepta el encuentro propuesto
    @PostMapping("/confirmar")
    public ResponseEntity<?> confirmar(@PathVariable Long pedidoId, @PathVariable Long vendedorId, Authentication auth) {
        User usuario = resolverUsuario(auth);
        Pedido pedido = pedidoRepository.findByIdWithDetalles(pedidoId).orElse(null);
        if (pedido == null) throw new RecursoNoEncontradoException("Pedido no encontrado");
        if (!autorizado(pedido, vendedorId, usuario)) {
            throw new NoAutorizadoException("No autorizado");
        }

        Encuentro encuentro = encuentroRepository.findByPedidoIdAndVendedorId(pedidoId, vendedorId).orElse(null);
        if (encuentro == null) throw new RecursoNoEncontradoException("Encuentro no encontrado");
        if (usuario.getId().equals(encuentro.getPropuestoPor().getId())) {
            throw new SolicitudInvalidaException("No podés confirmar tu propia propuesta");
        }
        if ("CONFIRMADO".equals(encuentro.getEstado())) {
            throw new SolicitudInvalidaException("El encuentro ya está confirmado");
        }

        encuentro.setEstado("CONFIRMADO");
        encuentro.setFechaActualizacion(LocalDateTime.now());
        encuentroRepository.save(encuentro);

        notificarOtraParte(pedido, vendedorId, usuario,
                usuario.getUsername() + " confirmó el encuentro del pedido #" + pedidoId,
                "ENCUENTRO_CONFIRMADO", pedidoId);

        return ResponseEntity.ok(toMap(encuentro, usuario, pedidoId, vendedorId));
    }

    // DELETE /api/pedidos/{pedidoId}/encuentro/{vendedorId} — cancela el encuentro acordado
    @DeleteMapping
    public ResponseEntity<?> cancelar(@PathVariable Long pedidoId, @PathVariable Long vendedorId, Authentication auth) {
        User usuario = resolverUsuario(auth);
        Pedido pedido = pedidoRepository.findByIdWithDetalles(pedidoId).orElse(null);
        if (pedido == null) throw new RecursoNoEncontradoException("Pedido no encontrado");
        if (!autorizado(pedido, vendedorId, usuario)) {
            throw new NoAutorizadoException("No autorizado");
        }

        Encuentro encuentro = encuentroRepository.findByPedidoIdAndVendedorId(pedidoId, vendedorId).orElse(null);
        if (encuentro == null) throw new RecursoNoEncontradoException("Encuentro no encontrado");

        encuentroRepository.delete(encuentro);

        notificarOtraParte(pedido, vendedorId, usuario,
                usuario.getUsername() + " canceló el encuentro del pedido #" + pedidoId,
                "ENCUENTRO_CANCELADO", pedidoId);

        return ResponseEntity.ok(Map.of("cancelado", true));
    }

    // Resuelve al destinatario opuesto al que hizo la acción y le manda notificación + correo.
    // Siempre recarga el destinatario completo vía userService (en vez de navegar la entidad
    // Pedido/vendedor ya cargada) para no arriesgar LazyInitializationException fuera de la
    // transacción que originalmente cargó esas relaciones — mismo problema documentado en
    // ChatApiController/PedidoApiController para accesos a User vía relaciones LAZY.
    private void notificarOtraParte(Pedido pedido, Long vendedorId, User usuario, String mensaje, String tipo, Long pedidoId) {
        boolean esParaComprador = !usuario.getId().equals(pedido.getUsuario().getId());
        Long destinatarioId = esParaComprador ? pedido.getUsuario().getId() : vendedorId;
        User destinatario = userService.buscarPorId(destinatarioId).orElse(null);
        if (destinatario == null) return;

        notificacionService.crear(destinatario, mensaje, tipo, pedidoId, null, vendedorId,
                esParaComprador ? "COMPRADOR" : "VENDEDOR");
        if (destinatario.getEmail() != null && !destinatario.getEmail().isBlank()) {
            String link = frontendLinkBuilder.pedidoConFoco(!esParaComprador, pedidoId, vendedorId, "encuentro");
            emailService.enviarCorreo(destinatario.getEmail(), "Encuentro del pedido #" + pedidoId + " - MarketS",
                    mensaje + "\n\nVer en MarketS: " + link);
        }
    }

    private boolean autorizado(Pedido pedido, Long vendedorId, User usuario) {
        boolean esAdmin = "ROLE_ADMIN".equals(usuario.getRol().getRolname());
        boolean esComprador = pedido.getUsuario().getId().equals(usuario.getId());
        boolean esVendedorDelPedido = usuario.getId().equals(vendedorId)
                && pedido.getDetalles().stream()
                        .anyMatch(d -> d.getProducto().getVendedor() != null
                                && d.getProducto().getVendedor().getId().equals(vendedorId));
        return esAdmin || esComprador || esVendedorDelPedido;
    }

    private User resolverUsuario(Authentication auth) {
        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) throw new RecursoNoEncontradoException("Usuario no encontrado: " + (auth != null ? auth.getName() : "anónimo"));
        return usuario;
    }

    // puedeConfirmar: calculado server-side (en vez de que el frontend compare ids) porque el
    // frontend solo tiene el email/rol del usuario logueado en su AuthContext, no su id.
    // ventaExternaConfirmada: le permite al frontend, una vez que pasó la fecha del encuentro,
    // distinguir "venta realizada" (el vendedor ya marcó completado) de "la fecha caducó sin
    // que nadie confirmara nada" — no aplica a pedidos pagados por MercadoPago, donde nunca
    // existe una ResolucionVentaExterna y por lo tanto siempre da false.
    private Map<String, Object> toMap(Encuentro e, User usuarioActual, Long pedidoId, Long vendedorId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", e.getId());
        map.put("lugar", e.getLugar());
        map.put("fechaHora", e.getFechaHora().toString());
        map.put("nota", e.getNota());
        map.put("estado", e.getEstado());
        map.put("propuestoPorId", e.getPropuestoPor().getId());
        map.put("propuestoPorUsername", e.getPropuestoPor().getUsername());
        boolean puedeConfirmar = "PENDIENTE".equals(e.getEstado())
                && !usuarioActual.getId().equals(e.getPropuestoPor().getId());
        map.put("puedeConfirmar", puedeConfirmar);
        boolean ventaExternaConfirmada = resolucionVentaExternaRepository.findByPedidoIdAndVendedorId(pedidoId, vendedorId)
                .map(ResolucionVentaExterna::isCompletado)
                .orElse(false);
        map.put("ventaExternaConfirmada", ventaExternaConfirmada);
        return map;
    }
}
