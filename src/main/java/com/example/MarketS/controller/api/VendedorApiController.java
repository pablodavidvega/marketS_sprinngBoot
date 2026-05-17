package com.example.MarketS.controller.api;

import com.example.MarketS.dto.ProductoResponseDTO;
import com.example.MarketS.dto.TallasStockDTO;
import com.example.MarketS.model.*;
import com.example.MarketS.repository.*;
import com.example.MarketS.service.ProductoService;
import com.example.MarketS.service.UserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

@RestController
@RequestMapping("/api/vendedor")
@PreAuthorize("hasAnyAuthority('ROLE_VENDEDOR', 'ROLE_ADMIN')")
public class VendedorApiController {

    @Value("${app.upload.path:/tmp/uploads/}")
    private String uploadPath;

    private final ProductoService productoService;
    private final ProductoRepository productoRepository;
    private final UserService userService;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;

    public VendedorApiController(ProductoService productoService,
                                  ProductoRepository productoRepository,
                                  UserService userService,
                                  CategoriaRepository categoriaRepository,
                                  MarcaRepository marcaRepository) {
        this.productoService = productoService;
        this.productoRepository = productoRepository;
        this.userService = userService;
        this.categoriaRepository = categoriaRepository;
        this.marcaRepository = marcaRepository;
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
        return productoRepository.findById(id.intValue())
                .filter(p -> p.getVendedor().equals(usuario))
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
            @RequestParam String descripcion,
            @RequestParam String estadoProducto,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaAgregacion,
            @RequestParam @NotNull Long categoriaId,
            @RequestParam @NotBlank String nombreMarca,
            @RequestParam(required = false) Integer stock,
            @RequestParam(required = false) MultipartFile img1,
            @RequestParam(required = false) MultipartFile img2,
            @RequestParam(required = false) MultipartFile img3,
            @RequestParam(required = false) MultipartFile video) throws IOException {

        User usuario = resolverUsuario(auth);

        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setDescripcion(descripcion);
        producto.setEstadoProducto(estadoProducto);
        producto.setFechaAgregacion(fechaAgregacion);
        producto.setVendedor(usuario);
        producto.setStock(stock);

        aplicarCategoria(producto, categoriaId);
        aplicarMarca(producto, nombreMarca);

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
            @RequestParam String descripcion,
            @RequestParam String estadoProducto,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaAgregacion,
            @RequestParam @NotNull Long categoriaId,
            @RequestParam @NotBlank String nombreMarca,
            @RequestParam(required = false) Integer stock,
            @RequestParam(required = false) MultipartFile img1,
            @RequestParam(required = false) MultipartFile img2,
            @RequestParam(required = false) MultipartFile img3,
            @RequestParam(required = false) MultipartFile video,
            @RequestParam(required = false) List<String> tallas,
            @RequestParam(required = false) List<Integer> tallaStocks) throws IOException {

        User usuario = resolverUsuario(auth);
        Producto producto = productoRepository.findById(id.intValue()).orElse(null);

        if (producto == null || !producto.getVendedor().equals(usuario)) {
            return ResponseEntity.notFound().build();
        }

        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setDescripcion(descripcion);
        producto.setEstadoProducto(estadoProducto);
        producto.setFechaAgregacion(fechaAgregacion);

        if (stock != null && stock >= 0) {
            producto.setStock(stock);
            if (stock == 0) producto.setEstadoProducto("No disponible");
        }

        aplicarCategoria(producto, categoriaId);
        aplicarMarca(producto, nombreMarca);

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
        Producto producto = productoRepository.findById(id.intValue()).orElse(null);
        if (producto == null || !producto.getVendedor().equals(usuario)) {
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
        categoriaRepository.findById(categoriaId).ifPresent(producto::setCategorias);
    }

    private void aplicarMarca(Producto producto, String nombreMarca) {
        Marca marca = marcaRepository.findByNombreMarca(nombreMarca)
                .orElseGet(() -> marcaRepository.save(new Marca(nombreMarca)));
        producto.setMarca(marca);
    }

    private String guardarArchivo(MultipartFile archivo, String prefijo) throws IOException {
        String originalFilename = archivo.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : "";
        String nombreArchivo = prefijo.replaceAll("\\s+", "_").toLowerCase()
                + "_" + System.currentTimeMillis() + extension;

        String pathNormalizado = uploadPath.startsWith("file:")
                ? uploadPath.substring(5)
                : uploadPath;
        Path ruta = Paths.get(pathNormalizado);
        if (!Files.exists(ruta)) Files.createDirectories(ruta);
        Files.copy(archivo.getInputStream(), ruta.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);

        return nombreArchivo;
    }
}
