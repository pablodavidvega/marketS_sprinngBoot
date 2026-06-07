package com.example.MarketS.controller.api;

import com.example.MarketS.dto.ProductoResponseDTO;
import com.example.MarketS.dto.TallasStockDTO;
import com.example.MarketS.model.*;
import com.example.MarketS.repository.*;
import com.example.MarketS.service.ProductoService;
import com.example.MarketS.service.UserService;
import com.example.MarketS.service.CloudStorageService;
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
    private final UserService userService;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final CloudStorageService cloudStorageService;
    private final DetallePedidoRepository detallePedidoRepository;

    public VendedorApiController(ProductoService productoService,
                                  ProductoRepository productoRepository,
                                  UserService userService,
                                  CategoriaRepository categoriaRepository,
                                  MarcaRepository marcaRepository,
                                  CloudStorageService cloudStorageService,
                                  DetallePedidoRepository detallePedidoRepository) {
        this.productoService = productoService;
        this.productoRepository = productoRepository;
        this.userService = userService;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
        this.cloudStorageService = cloudStorageService;
        this.detallePedidoRepository = detallePedidoRepository;
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
                "totalProductos", totalProductos
        ));
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

        List<ProductoResponseDTO> contenido = paginado.getContent().stream()
                .map(ProductoResponseDTO::from)
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
                .map(ProductoResponseDTO::from)
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
            @RequestParam(required = false) Long marcaId,
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
        aplicarMarca(producto, marcaId);

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
            @RequestParam(required = false) Long marcaId,
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
        aplicarMarca(producto, marcaId);

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
        return ResponseEntity.ok(ProductoResponseDTO.from(producto));
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
        return ResponseEntity.ok(ProductoResponseDTO.from(producto));
    }

    private User resolverUsuario(Authentication auth) {
        return userService.buscarPorEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + auth.getName()));
    }

    private void aplicarCategoria(Producto producto, Long categoriaId) {
        if (categoriaId != null) {
            categoriaRepository.findById(categoriaId).ifPresent(producto::setCategorias);
        } else {
            producto.setCategorias(null);
        }
    }

    private void aplicarMarca(Producto producto, Long marcaId) {
        if (marcaId != null) {
            marcaRepository.findById(marcaId).ifPresent(producto::setMarca);
        } else {
            producto.setMarca(null);
        }
    }

    private String guardarArchivo(MultipartFile archivo, String prefijo) throws IOException {
        return cloudStorageService.subirArchivo(archivo, "");
    }
}
