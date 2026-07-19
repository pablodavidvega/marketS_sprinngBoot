package com.example.MarketS.controller.api;

import com.example.MarketS.dto.PedidoVendedorDTO;
import com.example.MarketS.dto.ProductoResponseDTO;
import com.example.MarketS.dto.ReputacionDTO;
import com.example.MarketS.dto.TallasStockDTO;
import com.example.MarketS.model.*;
import com.example.MarketS.repository.*;
import com.example.MarketS.service.ProductoService;
import com.example.MarketS.service.CloudStorageService;
import com.example.MarketS.service.ReputacionService;
import com.example.MarketS.security.UsuarioPrincipal;
import com.example.MarketS.repository.DetallePedidoRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/vendedor")
@PreAuthorize("hasAnyAuthority('ROLE_VENDEDOR', 'ROLE_ADMIN')")
public class VendedorApiController {

    private final ProductoService productoService;
    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final CloudStorageService cloudStorageService;
    private final DetallePedidoRepository detallePedidoRepository;
    private final UserRepository userRepository;
    private final ResenaCompradorRepository resenaCompradorRepository;
    private final ReputacionService reputacionService;

    public VendedorApiController(ProductoService productoService,
                                  ProductoRepository productoRepository,
                                  PedidoRepository pedidoRepository,
                                  CategoriaRepository categoriaRepository,
                                  MarcaRepository marcaRepository,
                                  CloudStorageService cloudStorageService,
                                  DetallePedidoRepository detallePedidoRepository,
                                  UserRepository userRepository,
                                  ResenaCompradorRepository resenaCompradorRepository,
                                  ReputacionService reputacionService) {
        this.productoService = productoService;
        this.productoRepository = productoRepository;
        this.pedidoRepository = pedidoRepository;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.cloudStorageService = cloudStorageService;
        this.detallePedidoRepository = detallePedidoRepository;
        this.userRepository = userRepository;
        this.resenaCompradorRepository = resenaCompradorRepository;
        this.reputacionService = reputacionService;
    }

    // GET /api/vendedor/dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(Authentication auth) {
        User usuario = resolverUsuario(auth);
        long totalProductos = productoRepository.countByVendedorId(usuario.getId());
        return ResponseEntity.ok(Map.of(
                "gananciaTotal",  detallePedidoRepository.sumGananciaByVendedor(usuario),
                "comisionTotal",  detallePedidoRepository.sumComisionByVendedor(usuario),
                "totalVentas",    detallePedidoRepository.countVentasByVendedor(usuario),
                "totalProductos", totalProductos,
                "reputacion",     reputacionService.calcularVendedor(usuario.getId())
        ));
    }

    // GET /api/vendedor/reportes?periodo=30d
    @GetMapping("/reportes")
    public ResponseEntity<Map<String, Object>> reportes(
            @RequestParam(defaultValue = "30d") String periodo,
            Authentication auth) {

        User vendedor = resolverUsuario(auth);
        LocalDateTime desde = calcularDesde(periodo);

        List<Object[]> rawProductos = detallePedidoRepository.findVentasPorProducto(vendedor, desde);
        List<Map<String, Object>> ventasPorProducto = rawProductos.stream().map(row -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("nombre",   row[0] != null ? (String) row[0] : "");
            m.put("unidades", row[1] != null ? ((Number) row[1]).longValue()   : 0L);
            m.put("ganancia", row[2] != null ? ((Number) row[2]).doubleValue() : 0.0);
            m.put("ingresos", row[3] != null ? ((Number) row[3]).doubleValue() : 0.0);
            return m;
        }).toList();

        List<Object[]> rawMes = detallePedidoRepository.findVentasPorMes(vendedor, desde);
        List<Map<String, Object>> ventasPorMes = rawMes.stream().map(row -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("periodo",  String.format("%d-%02d",
                    row[0] != null ? ((Number) row[0]).intValue() : 0,
                    row[1] != null ? ((Number) row[1]).intValue() : 0));
            m.put("ganancia", row[2] != null ? ((Number) row[2]).doubleValue() : 0.0);
            m.put("unidades", row[3] != null ? ((Number) row[3]).longValue()   : 0L);
            return m;
        }).toList();

        double gananciaTotal  = ventasPorProducto.stream().mapToDouble(p -> (Double) p.get("ganancia")).sum();
        long   totalUnidades  = ventasPorProducto.stream().mapToLong(p -> (Long) p.get("unidades")).sum();
        double ingresosTotal  = ventasPorProducto.stream().mapToDouble(p -> (Double) p.get("ingresos")).sum();
        String productoTop    = ventasPorProducto.isEmpty() ? "—" : (String) ventasPorProducto.get(0).get("nombre");
        double ticketPromedio = totalUnidades > 0 ? ingresosTotal / totalUnidades : 0;

        Map<String, Object> resumen = new java.util.HashMap<>();
        resumen.put("gananciaTotal",  gananciaTotal);
        resumen.put("totalUnidades",  totalUnidades);
        resumen.put("productoTop",    productoTop != null ? productoTop : "—");
        resumen.put("ticketPromedio", ticketPromedio);

        Map<String, Object> respuesta = new java.util.HashMap<>();
        respuesta.put("resumen",           resumen);
        respuesta.put("ventasPorProducto", ventasPorProducto);
        respuesta.put("ventasPorMes",      ventasPorMes);

        return ResponseEntity.ok(respuesta);
    }

    // GET /api/vendedor/pedidos
    @GetMapping("/pedidos")
    public ResponseEntity<List<PedidoVendedorDTO>> pedidosRecibidos(Authentication auth) {
        User vendedor = resolverUsuario(auth);
        List<Pedido> pedidos = pedidoRepository.findByVendedorWithDetails(vendedor);

        // Resuelve en 1 sola query los datos de contacto de todos los compradores distintos
        // (evita cargar el User completo de cada uno, ver Pedido.usuario).
        List<Long> compradorIds = pedidos.stream()
                .map(p -> p.getUsuario().getId())
                .distinct()
                .toList();
        Map<Long, PedidoVendedorDTO.CompradorInfo> compradores = new java.util.HashMap<>();
        if (!compradorIds.isEmpty()) {
            userRepository.findContactInfoByIdIn(compradorIds).forEach(row ->
                    compradores.put(
                            ((Number) row[0]).longValue(),
                            new PedidoVendedorDTO.CompradorInfo(
                                    (String) row[1], (String) row[2], (String) row[3], (String) row[4])));
        }

        // Reputación de cada comprador distinto — batch (ver ReputacionService), no una
        // consulta por comprador.
        Map<Long, ReputacionDTO> reputaciones = reputacionService.calcularCompradoresBatch(compradorIds);

        // Pedidos que este vendedor ya calificó — 1 sola query batch.
        List<Long> pedidoIds = pedidos.stream().map(Pedido::getId).toList();
        java.util.Set<Long> pedidosCalificados = pedidoIds.isEmpty()
                ? java.util.Set.of()
                : new java.util.HashSet<>(resenaCompradorRepository.findPedidoIdsCalificados(vendedor.getId(), pedidoIds));

        List<PedidoVendedorDTO> result = pedidos.stream()
                .map(p -> PedidoVendedorDTO.from(p, vendedor, compradores, reputaciones, pedidosCalificados))
                .toList();
        return ResponseEntity.ok(result);
    }

    // GET /api/vendedor/productos?page=0&size=20&keyword=x
    @GetMapping("/productos")
    public ResponseEntity<Map<String, Object>> listarProductos(
            Authentication auth,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        User usuario = resolverUsuario(auth);
        Page<Producto> paginado = keyword != null && !keyword.isBlank()
                ? productoService.buscarPorTodosLosCampos(keyword, usuario.getId(), PageRequest.of(page, size))
                : productoService.listarProductosPorVendedor(usuario.getId(), PageRequest.of(page, size));

        // Todos los productos de esta página son del mismo "usuario" (se filtra por vendedor
        // arriba), así que no hace falta resolverlo en batch — ya lo conocemos.
        Map<Long, String> vendedorUsernames = Map.of(usuario.getId(), usuario.getUsername());
        List<ProductoResponseDTO> contenido = paginado.getContent().stream()
                .map(p -> ProductoResponseDTO.from(p, null, null, null, vendedorUsernames))
                .toList();

        return ResponseEntity.ok(Map.of(
                "contenido", contenido,
                "paginaActual", paginado.getNumber(),
                "totalPaginas", paginado.getTotalPages(),
                "totalElementos", paginado.getTotalElements()
        ));
    }

    // GET /api/vendedor/productos/{id}
    @GetMapping("/productos/{id}")
    public ResponseEntity<ProductoResponseDTO> detalle(@PathVariable Long id, Authentication auth) {
        User usuario = resolverUsuario(auth);
        return productoRepository.findById(id)
                .filter(p -> p.getVendedor().getId().equals(usuario.getId()))
                .map(p -> ProductoResponseDTO.from(p, null, null, null, Map.of(usuario.getId(), usuario.getUsername())))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/vendedor/productos  (multipart/form-data)
    @PostMapping("/productos")
    public ResponseEntity<ProductoResponseDTO> crear(
            Authentication auth,
            @RequestParam @NotBlank String nombre,
            @RequestParam @NotNull @Positive Double precio,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false, defaultValue = "") String estadoProducto,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaAgregacion,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) Integer stock,
            @RequestParam(value = "imagen", required = false) MultipartFile img1,
            @RequestParam(value = "imagen2", required = false) MultipartFile img2,
            @RequestParam(value = "imagen3", required = false) MultipartFile img3,
            @RequestParam(required = false) MultipartFile video) throws IOException {

        User usuario = resolverUsuario(auth);

        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setDescripcion(descripcion);
        producto.setEstadoProducto(estadoProducto != null && !estadoProducto.isBlank() ? estadoProducto : "Disponible");
        producto.setFechaAgregacion(fechaAgregacion != null ? fechaAgregacion : new Date());
        producto.setVendedor(usuario);
        producto.setStock(stock != null ? stock : 0);
        producto.setActivo(true);

        aplicarCategoria(producto, categoriaId);
        aplicarMarca(producto, marca);

        if (img1 != null && !img1.isEmpty()) producto.setImagen(guardarArchivo(img1, nombre));
        if (img2 != null && !img2.isEmpty()) producto.setImagen2(guardarArchivo(img2, nombre));
        if (img3 != null && !img3.isEmpty()) producto.setImagen3(guardarArchivo(img3, nombre));
        if (video != null && !video.isEmpty()) producto.setVideo(guardarArchivo(video, nombre + "_video"));

        productoRepository.save(producto);
        return ResponseEntity.ok(ProductoResponseDTO.from(producto));
    }

    // PUT /api/vendedor/productos/{id}  (multipart/form-data)
    @PutMapping("/productos/{id}")
    public ResponseEntity<ProductoResponseDTO> actualizar(
            @PathVariable Long id,
            Authentication auth,
            @RequestParam @NotBlank String nombre,
            @RequestParam @NotNull @Positive Double precio,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false, defaultValue = "") String estadoProducto,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaAgregacion,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) Integer stock,
            @RequestParam(value = "imagen", required = false) MultipartFile img1,
            @RequestParam(value = "imagen2", required = false) MultipartFile img2,
            @RequestParam(value = "imagen3", required = false) MultipartFile img3,
            @RequestParam(required = false) MultipartFile video,
            @RequestParam(required = false) List<String> tallas,
            @RequestParam(required = false) List<Integer> tallaStocks) throws IOException {

        User usuario = resolverUsuario(auth);
        Producto producto = productoRepository.findById(id).orElse(null);

        if (producto == null || !producto.getVendedor().getId().equals(usuario.getId())) {
            return ResponseEntity.notFound().build();
        }

        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setDescripcion(descripcion);
        if (estadoProducto != null && !estadoProducto.isBlank()) {
            producto.setEstadoProducto(estadoProducto);
        }
        if (fechaAgregacion != null) {
            producto.setFechaAgregacion(fechaAgregacion);
        }

        if (stock != null && stock >= 0) {
            producto.setStock(stock);
            if (stock == 0) producto.setEstadoProducto("No disponible");
        }

        aplicarCategoria(producto, categoriaId);
        aplicarMarca(producto, marca);

        if (img1 != null && !img1.isEmpty()) producto.setImagen(guardarArchivo(img1, nombre));
        if (img2 != null && !img2.isEmpty()) producto.setImagen2(guardarArchivo(img2, nombre));
        if (img3 != null && !img3.isEmpty()) producto.setImagen3(guardarArchivo(img3, nombre));
        if (video != null && !video.isEmpty()) producto.setVideo(guardarArchivo(video, nombre + "_video"));

        if (tallas != null && tallaStocks != null) {
            producto.getTallas().clear();
            for (int i = 0; i < tallas.size(); i++) {
                String talla = tallas.get(i);
                if (talla != null && !talla.isBlank()) {
                    TallaStock ts = new TallaStock();
                    ts.setTalla(talla);
                    ts.setStock(i < tallaStocks.size() && tallaStocks.get(i) != null ? tallaStocks.get(i) : 0);
                    ts.setProducto(producto);
                    producto.getTallas().add(ts);
                }
            }
        }

        productoRepository.save(producto);
        // producto.getVendedor() es LAZY y este producto se recargó vía findById (sesión ya
        // cerrada), pero ya sabemos que el vendedor es "usuario" (se validó arriba).
        return ResponseEntity.ok(ProductoResponseDTO.from(
                producto, null, null, null, Map.of(usuario.getId(), usuario.getUsername())));
    }

    // PATCH /api/vendedor/productos/{id}/activar
    @PatchMapping("/productos/{id}/activar")
    public ResponseEntity<ProductoResponseDTO> activar(@PathVariable Long id, Authentication auth) {
        return cambiarEstadoActivo(id, auth, true);
    }

    // PATCH /api/vendedor/productos/{id}/desactivar
    @PatchMapping("/productos/{id}/desactivar")
    public ResponseEntity<ProductoResponseDTO> desactivar(@PathVariable Long id, Authentication auth) {
        return cambiarEstadoActivo(id, auth, false);
    }

    // --- helpers privados ---

    private ResponseEntity<ProductoResponseDTO> cambiarEstadoActivo(Long id, Authentication auth, boolean activo) {
        User usuario = resolverUsuario(auth);
        Producto producto = productoRepository.findById(id).orElse(null);
        if (producto == null || !producto.getVendedor().getId().equals(usuario.getId())) {
            return ResponseEntity.notFound().build();
        }
        producto.setActivo(activo);
        productoRepository.save(producto);
        // Igual que en actualizar(): producto.getVendedor() es LAZY, pero ya sabemos que es "usuario".
        return ResponseEntity.ok(ProductoResponseDTO.from(
                producto, null, null, null, Map.of(usuario.getId(), usuario.getUsername())));
    }

    private LocalDateTime calcularDesde(String periodo) {
        LocalDateTime ahora = LocalDateTime.now();
        return switch (periodo) {
            case "7d"  -> ahora.minusDays(7);
            case "30d" -> ahora.minusDays(30);
            case "3m"  -> ahora.minusMonths(3);
            case "6m"  -> ahora.minusMonths(6);
            case "1y"  -> ahora.minusYears(1);
            default    -> LocalDateTime.of(2000, 1, 1, 0, 0);
        };
    }

    private User resolverUsuario(Authentication auth) {
        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) throw new RuntimeException("Usuario no encontrado: " + (auth != null ? auth.getName() : "anónimo"));
        return usuario;
    }

    private void aplicarCategoria(Producto producto, Long categoriaId) {
        if (categoriaId != null) {
            categoriaRepository.findById(categoriaId).ifPresent(producto::setCategorias);
        } else {
            producto.setCategorias(null);
        }
    }

    // El vendedor escribe la marca en un input libre (con autocompletado de las ya existentes
    // en el frontend): se busca sin distinguir mayúsculas/minúsculas para no duplicar
    // "Nike"/"nike", y si no existe se crea al vuelo — evita que marca quede como catálogo
    // cerrado que solo el admin puede ampliar (hoy no hay ningún endpoint para crear marcas).
    private void aplicarMarca(Producto producto, String marcaNombre) {
        if (marcaNombre == null || marcaNombre.isBlank()) {
            producto.setMarca(null);
            return;
        }
        String nombre = marcaNombre.trim();
        Marca marca = marcaRepository.findByNombreMarcaIgnoreCase(nombre)
                .orElseGet(() -> marcaRepository.save(new Marca(nombre)));
        producto.setMarca(marca);
    }

    private String guardarArchivo(MultipartFile archivo, String prefijo) throws IOException {
        return cloudStorageService.subirArchivo(archivo, "");
    }
}
