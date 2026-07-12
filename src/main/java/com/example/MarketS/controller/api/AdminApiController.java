package com.example.MarketS.controller.api;

import com.example.MarketS.dto.ComunidadEstadisticasDTO;
import com.example.MarketS.dto.EmbudoAdminDTO;
import com.example.MarketS.dto.PerfilResponseDTO;
import com.example.MarketS.dto.RegistroUsuarioDTO;
import com.example.MarketS.dto.ReporteAdminDTO;
import com.example.MarketS.dto.ReputacionDTO;
import com.example.MarketS.dto.VendedorEstadisticasDTO;
import com.example.MarketS.model.Rol;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.EncuentroRepository;
import com.example.MarketS.repository.FavoritoRepository;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.repository.ProductoRepository;
import com.example.MarketS.repository.RolRepository;
import com.example.MarketS.repository.UserRepository;
import com.example.MarketS.repository.VendedorRepository;
import com.example.MarketS.service.ReporteAdminService;
import com.example.MarketS.service.ReportePdfService;
import com.example.MarketS.service.ReputacionService;
import com.example.MarketS.service.UserService;
import com.example.MarketS.service.reportes.ReporteContexto;
import com.example.MarketS.service.reportes.ReporteExcelStrategy;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminApiController {

    private static final List<String> ESTADOS_VENTA_CONCRETADA = List.of("PAGADO", "RECIBIDO", "COMPLETADO_EXTERNO");
    private static final List<String> ESTADOS_CHECKOUT_SIN_RESOLVER = List.of("PENDIENTE", "PROCESANDO_PAGO", "PENDIENTE_PAGO");
    private static final int HORAS_PARA_CONSIDERAR_ABANDONADO = 24;
    private static final int SEMANAS_REGISTROS = 8;

    private final UserService userService;
    private final RolRepository rolRepository;
    private final UserRepository userRepository;
    private final ReputacionService reputacionService;
    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;
    private final VendedorRepository vendedorRepository;
    private final FavoritoRepository favoritoRepository;
    private final EncuentroRepository encuentroRepository;

    @Autowired
    private ReporteAdminService reporteAdminService;

    @Autowired
    private ReporteContexto reporteContexto;

    @Autowired
    private ReportePdfService reportePdfService;

    public AdminApiController(UserService userService, RolRepository rolRepository, UserRepository userRepository,
                               ReputacionService reputacionService, ProductoRepository productoRepository,
                               PedidoRepository pedidoRepository, VendedorRepository vendedorRepository,
                               FavoritoRepository favoritoRepository, EncuentroRepository encuentroRepository) {
        this.userService = userService;
        this.rolRepository = rolRepository;
        this.userRepository = userRepository;
        this.reputacionService = reputacionService;
        this.productoRepository = productoRepository;
        this.pedidoRepository = pedidoRepository;
        this.vendedorRepository = vendedorRepository;
        this.favoritoRepository = favoritoRepository;
        this.encuentroRepository = encuentroRepository;
    }

    // GET /api/admin/dashboard
    // @Transactional: agrupa todas las consultas de este método en una sola conexión/transacción
    // en vez de una por cada llamada al repositorio — con una BD remota (pooler), abrir y cerrar
    // conexión por consulta es el costo dominante, no la cantidad de queries en sí.
    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(Map.of(
                "totalUsuarios",   userService.contarTodos(),
                "totalClientes",   userService.contarPorRol("ROLE_CLIENTE"),
                "totalVendedores", userService.contarPorRol("ROLE_VENDEDOR"),
                "totalAdmins",     userService.contarPorRol("ROLE_ADMIN"),
                "reputacionVendedores",  distribucionReputacion("ROLE_VENDEDOR", true),
                "reputacionCompradores", distribucionReputacion("ROLE_CLIENTE", false),
                "totalFavoritos",      favoritoRepository.count(),
                "productoMasGuardado", productoMasGuardado()
        ));
    }

    // Producto con más favoritos guardados en toda la plataforma. Map.of(...) no admite valores
    // null, así que si todavía nadie ha guardado nada se devuelve un mapa vacío en vez de null
    // (el frontend lo trata igual: ausencia de "id" = sin datos aún).
    private Map<String, Object> productoMasGuardado() {
        List<Object[]> top = favoritoRepository.findProductosMasGuardados(PageRequest.of(0, 1));
        if (top.isEmpty()) return Map.of();
        Object[] row = top.get(0);
        return Map.of(
                "id", ((Number) row[0]).longValue(),
                "nombre", (String) row[1],
                "totalFavoritos", ((Number) row[2]).longValue()
        );
    }

    // Cuenta cuántos usuarios de un rol caen en cada nivel de reputación (incluye "NUEVO").
    // Usa los métodos batch de ReputacionService (un puñado de queries fijas, no una por
    // usuario) — con muchos usuarios, calcular uno por uno volvía lento el panel de admin.
    private Map<String, Long> distribucionReputacion(String rolname, boolean comoVendedor) {
        List<Long> ids = userRepository.findIdsByRolRolname(rolname);
        Map<Long, ReputacionDTO> reputaciones = comoVendedor
                ? reputacionService.calcularVendedoresBatch(ids)
                : reputacionService.calcularCompradoresBatch(ids);

        Map<String, Long> distribucion = new java.util.LinkedHashMap<>();
        reputaciones.values().forEach(rep -> {
            String nivel = "NUEVO".equals(rep.estado()) ? "NUEVO" : rep.nivel();
            distribucion.merge(nivel, 1L, Long::sum);
        });
        return distribucion;
    }

    // GET /api/admin/estadisticas/embudo — adopción de la comunidad: registros, quién publica,
    // quién compra, abandono de checkout y tiempo registro→primera publicación.
    @GetMapping("/estadisticas/embudo")
    @Transactional(readOnly = true)
    public ResponseEntity<EmbudoAdminDTO> embudo() {
        long totalVendedores = userRepository.countByRolRolname("ROLE_VENDEDOR");
        long totalRegistrados = userRepository.countByRolRolname("ROLE_CLIENTE") + totalVendedores;
        long totalPublicaron = productoRepository.countDistinctVendedores();
        long totalCompraron = pedidoRepository.countDistinctUsuarioByEstadoIn(ESTADOS_VENTA_CONCRETADA);

        LocalDateTime antesDe = LocalDateTime.now().minusHours(HORAS_PARA_CONSIDERAR_ABANDONADO);
        long pedidosElegibles = pedidoRepository.countByFechaBefore(antesDe);
        long pedidosAbandonados = pedidoRepository.countByFechaBeforeAndEstadoIn(antesDe, ESTADOS_CHECKOUT_SIN_RESOLVER);
        Double tasaAbandonoCheckout = pedidosElegibles > 0 ? pedidosAbandonados / (double) pedidosElegibles : null;

        Double diasPromedioRegistroAPublicacion = userRepository.avgDiasRegistroAPublicacion();

        LocalDateTime desde = LocalDateTime.now().minusWeeks(SEMANAS_REGISTROS);
        List<EmbudoAdminDTO.PuntoSemana> registrosPorSemana = userRepository.countRegistrosPorSemana(desde).stream()
                .map(row -> new EmbudoAdminDTO.PuntoSemana(row[0].toString(), ((Number) row[1]).longValue()))
                .toList();

        return ResponseEntity.ok(new EmbudoAdminDTO(
                totalRegistrados,
                totalVendedores,
                totalPublicaron,
                totalCompraron,
                pedidosElegibles,
                pedidosAbandonados,
                tasaAbandonoCheckout,
                diasPromedioRegistroAPublicacion,
                registrosPorSemana
        ));
    }

    // GET /api/admin/estadisticas/comunidad — uso y transacciones concretadas, pensado como
    // reporte para justificar el pago B2B ante quien administra la comunidad.
    @GetMapping("/estadisticas/comunidad")
    @Transactional(readOnly = true)
    public ResponseEntity<ComunidadEstadisticasDTO> estadisticasComunidad() {
        long totalVendedores = userRepository.countByRolRolname("ROLE_VENDEDOR");
        long totalRegistrados = userRepository.countByRolRolname("ROLE_CLIENTE") + totalVendedores;
        long totalTransaccionaron = pedidoRepository.countDistinctUsuarioByEstadoIn(ESTADOS_VENTA_CONCRETADA);
        Double tasaAdopcion = totalRegistrados > 0 ? totalTransaccionaron / (double) totalRegistrados : null;

        long totalTransacciones = pedidoRepository.countByEstadoIn(ESTADOS_VENTA_CONCRETADA);
        double gmvTotal = pedidoRepository.sumTotalByEstadoIn(ESTADOS_VENTA_CONCRETADA);

        LocalDateTime desde = LocalDateTime.now().minusWeeks(SEMANAS_REGISTROS);
        List<ComunidadEstadisticasDTO.PuntoSemana> transaccionesPorSemana = agruparPorSemana(
                pedidoRepository.findFechaYTotalByEstadoInAndFechaAfter(ESTADOS_VENTA_CONCRETADA, desde));

        return ResponseEntity.ok(new ComunidadEstadisticasDTO(
                totalRegistrados,
                totalTransaccionaron,
                tasaAdopcion,
                totalTransacciones,
                gmvTotal,
                favoritoRepository.count(),
                encuentroRepository.count(),
                transaccionesPorSemana
        ));
    }

    // Agrupa (fecha, monto) por semana —lunes de esa semana, mismo criterio que
    // DATE_TRUNC('week', ...) en Postgres— sumando monto y contando filas en memoria, para no
    // depender de una query nativa con funciones específicas del dialecto.
    private List<ComunidadEstadisticasDTO.PuntoSemana> agruparPorSemana(List<Object[]> filas) {
        Map<java.time.LocalDate, Long> conteo = new java.util.TreeMap<>();
        Map<java.time.LocalDate, Double> monto = new java.util.TreeMap<>();
        for (Object[] fila : filas) {
            LocalDateTime fecha = (LocalDateTime) fila[0];
            Double total = (Double) fila[1];
            java.time.LocalDate semana = fecha.toLocalDate()
                    .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            conteo.merge(semana, 1L, Long::sum);
            monto.merge(semana, total != null ? total : 0.0, Double::sum);
        }
        return conteo.entrySet().stream()
                .map(e -> new ComunidadEstadisticasDTO.PuntoSemana(
                        e.getKey().toString(), e.getValue(), monto.getOrDefault(e.getKey(), 0.0)))
                .toList();
    }

    // GET /api/admin/usuarios?tipo=email&valor=x
    @GetMapping("/usuarios")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PerfilResponseDTO>> listarUsuarios(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String valor) {

        // Caso más común (sin filtro, "ver todos"): proyección escalar, nunca instancia la
        // entidad User — evita el disparo automático de Hibernate al inicializar
        // User.vendedor en cada fila (ver UserRepository.findAllPerfilProjection).
        if (tipo == null || valor == null || valor.isBlank()) {
            List<Object[]> filas = userRepository.findAllPerfilProjection();

            List<Long> vendedorIds = filas.stream()
                    .filter(f -> "ROLE_VENDEDOR".equals(f[5]))
                    .map(f -> ((Number) f[0]).longValue())
                    .toList();
            Map<Long, String> ciudades = resolverCiudades(vendedorIds);

            List<PerfilResponseDTO> respuesta = filas.stream()
                    .map(f -> PerfilResponseDTO.fromProjection(f, ciudades.get(((Number) f[0]).longValue())))
                    .toList();
            return ResponseEntity.ok(respuesta);
        }

        // Búsquedas filtradas: resultados acotados (0-1 para nombre/email), se mantiene la
        // ruta por entidad completa.
        List<User> usuarios = switch (tipo) {
            case "nombre" -> userService.buscarPorNombre(valor).map(List::of).orElse(List.of());
            case "email"  -> userService.buscarPorEmail(valor).map(List::of).orElse(List.of());
            case "direccion" -> userService.buscarPorDireccion(valor);
            case "rol"    -> userService.listarPorRol("ROLE_" + valor.toUpperCase());
            default       -> userService.listarTodos();
        };

        List<Long> vendedorIds = usuarios.stream()
                .filter(u -> u.getRol() != null && "ROLE_VENDEDOR".equals(u.getRol().getRolname()))
                .map(User::getId)
                .toList();
        Map<Long, String> ciudades = resolverCiudades(vendedorIds);

        List<PerfilResponseDTO> respuesta = usuarios.stream()
                .map(u -> PerfilResponseDTO.from(u, ciudades.get(u.getId())))
                .toList();

        return ResponseEntity.ok(respuesta);
    }

    // Ciudad de cada vendedor, resuelta en batch (evita el N+1 de user.getVendedor()).
    private Map<Long, String> resolverCiudades(List<Long> vendedorIds) {
        Map<Long, String> ciudades = new java.util.HashMap<>();
        if (!vendedorIds.isEmpty()) {
            vendedorRepository.findCityByUserIdIn(vendedorIds).forEach(row ->
                    ciudades.put(((Number) row[0]).longValue(), (String) row[1]));
        }
        return ciudades;
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

    // GET /api/admin/reportes/usuarios — descarga PDF con la lista de usuarios (respeta los mismos filtros)
    @GetMapping("/reportes/usuarios")
    public ResponseEntity<byte[]> reporteUsuariosPdf(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String valor) {
        List<User> usuarios;
        if (tipo == null || valor == null || valor.isBlank()) {
            usuarios = userService.listarTodos();
        } else {
            usuarios = switch (tipo) {
                case "nombre"    -> userService.buscarPorNombre(valor).map(List::of).orElse(List.of());
                case "email"     -> userService.buscarPorEmail(valor).map(List::of).orElse(List.of());
                case "direccion" -> userService.buscarPorDireccion(valor);
                case "rol"       -> userService.listarPorRol("ROLE_" + valor.toUpperCase());
                default          -> userService.listarTodos();
            };
        }
        try {
            byte[] pdf = reportePdfService.generarReporteUsuarios(usuarios);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-usuarios.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // GET /api/admin/reportes/catalogo — datos del catálogo para el reporte React (sin imágenes base64)
    @GetMapping("/reportes/catalogo")
    public ResponseEntity<ReporteAdminDTO> reporteCatalogo() {
        ReporteAdminDTO reporte = reporteAdminService.generarReporte();
        reporte.setGraficoCategoriasBase64("");
        reporte.setGraficoPreciosStockBase64("");
        return ResponseEntity.ok(reporte);
    }

    // GET /api/admin/reportes/catalogo/excel — descarga Excel generado en el backend
    @GetMapping("/reportes/catalogo/excel")
    public void descargarExcelCatalogo(HttpServletResponse response) throws IOException {
        ReporteAdminDTO reporte = reporteAdminService.generarReporte();
        reporteContexto.setEstrategia(new ReporteExcelStrategy());
        reporteContexto.exportar(response, reporte);
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
