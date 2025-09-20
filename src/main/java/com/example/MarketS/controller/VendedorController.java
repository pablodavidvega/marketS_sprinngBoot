package com.example.MarketS.controller;

import com.example.MarketS.dto.ProductoDto;
import com.example.MarketS.dto.TallasStockDTO;
import com.example.MarketS.model.*;
import com.example.MarketS.repository.*;
import com.example.MarketS.service.PdfThymeleafService;
import com.example.MarketS.service.ProductoService;
import com.example.MarketS.service.UserService;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Controller
@RequestMapping("/vendedor")
public class VendedorController {

    private final Logger logg = LoggerFactory.getLogger(VendedorController.class);

    @Autowired
    private ProductoService productoService;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private PdfThymeleafService pdfThymeleafService;
    @Autowired
    private MarcaRepository marcaRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;


    @GetMapping("/dashboard")
    public String mostrarDashboard(Authentication authentication, Model model) {
        String email = authentication.getName();

        User usuario = userService.buscarPorEmail(email).orElse(null);
        model.addAttribute("usuario", usuario);
        return "vendedor/dashboard";
    }


    @GetMapping("/productos")
    public String listarProductos(
            Authentication authentication,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model) {

        String email = authentication.getName();
        User usuario = userService.buscarPorEmail(email).orElse(null);

        Page<Producto> productosPage = productoService.listarProductosPorVendedor(
                usuario.getId(), PageRequest.of(page, 30));

        model.addAttribute("productos", productosPage.getContent());
        model.addAttribute("page", productosPage);
        model.addAttribute("keyword", "");
        model.addAttribute("urlBase", "/vendedor/productos/buscar");
        model.addAttribute("usuario", usuario);

        return "vendedor/productos";
    }

    @GetMapping("/productos/buscar")
    public String buscarProductosPorKeyword(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Authentication authentication,
            Model model) {

        String email = authentication.getName();
        User usuario = userService.buscarPorEmail(email).orElse(null);

        Page<Producto> productosPage;

        if (keyword != null && !keyword.isBlank()) {
            productosPage = productoService.buscarPorTodosLosCampos(
                    keyword, usuario.getId(), PageRequest.of(page, 30));
        } else {
            productosPage = productoService.listarProductosPorVendedor(usuario.getId(), PageRequest.of(page, 30));
        }

        model.addAttribute("productos", productosPage.getContent());
        model.addAttribute("page", productosPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("urlBase", "/vendedor/productos/buscar");
        model.addAttribute("usuario", usuario);

        return "vendedor/productos";
    }

    /**
     * Formulario para agregar producto
     */
    @GetMapping("/productos/agregar")
    public String mostrarFormularioAgregarProducto(Authentication authentication, Model model) {
        String email = authentication.getName();
        User usuario = userService.buscarPorEmail(email).orElse(null);


        model.addAttribute("productoDto", new ProductoDto());
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("usuario", usuario);

        return "registerProducto";
    }

    @PostMapping("/productos/editar")
    public String actualizarProducto(
            @Valid @ModelAttribute("productoDto") ProductoDto productoDto,
            BindingResult result,
            Authentication authentication,
            Model model) throws Exception {

        String email = authentication.getName();
        User usuario = userService.buscarPorEmail(email).orElse(null);

        if (result.hasErrors()) {
            model.addAttribute("categorias", categoriaRepository.findAll());
            model.addAttribute("usuario", usuario);
            return "vendedor/edit";
        }

        Producto producto = productoRepository.findById(
                Integer.valueOf(productoDto.getId())
        ).orElse(null);
        if (producto == null || !producto.getVendedor().equals(usuario)) {
            return "redirect:/vendedor/productos";
        }

        producto.setNombre(productoDto.getNombre());
        producto.setPrecio(productoDto.getPrecio());
        producto.setDescripcion(productoDto.getDescripcion());
        producto.setEstadoProducto(productoDto.getEstadoProducto());
        producto.setFechaAgregacion(productoDto.getFechaAgregacion());
        producto.setStock(productoDto.getStock());

        if (productoDto.getCategoriaId() != null) {
            Categorias categoria = categoriaRepository.findById(productoDto.getCategoriaId()).orElse(null);
            producto.setCategorias(categoria);
        }

        if (productoDto.getNombreMarca() != null && !productoDto.getNombreMarca().isBlank()) {
            Marca marca = marcaRepository.findByNombreMarca(productoDto.getNombreMarca())
                    .orElseGet(() -> marcaRepository.save(new Marca(productoDto.getNombreMarca())));
            producto.setMarca(marca);
        }

        if (productoDto.getNuevaImagen() != null && !productoDto.getNuevaImagen().isEmpty()) {

            if (producto.getImagen() != null) {
                Path rutaAnterior = Paths.get("C:/uploads/hana").resolve(producto.getImagen());
                if (Files.exists(rutaAnterior)) {
                    Files.delete(rutaAnterior);
                }
            }

            String originalFilename = productoDto.getNuevaImagen().getOriginalFilename();
            String extension = "";
            int i = originalFilename.lastIndexOf('.');
            if (i >= 0) {
                extension = originalFilename.substring(i);
            }

            String nombreArchivo = productoDto.getNombre()
                    .replaceAll("\\s+", "_")
                    .toLowerCase() + extension;

            Path ruta = Paths.get("C:/uploads/hana");
            if (!Files.exists(ruta)) Files.createDirectories(ruta);

            Files.copy(productoDto.getNuevaImagen().getInputStream(),
                    ruta.resolve(nombreArchivo),
                    StandardCopyOption.REPLACE_EXISTING);

            producto.setImagen(nombreArchivo);
        }
        producto.getTallas().clear();

        if (productoDto.getTallas() != null) {
            for (TallasStockDTO tallaDto : productoDto.getTallas()) {
                if (tallaDto.getTalla() != null && !tallaDto.getTalla().isBlank()) {
                    TallaStock talla = new TallaStock();
                    talla.setTalla(tallaDto.getTalla());
                    talla.setStock(tallaDto.getStock() != null ? tallaDto.getStock() : 0);
                    talla.setProducto(producto);
                    producto.getTallas().add(talla);
                }
            }
        }



        productoRepository.save(producto);
        return "redirect:/vendedor/productos";
    }


    @PostMapping("/productos/guardar")
    public String guardarProducto(
            @Valid @ModelAttribute("productoDto") ProductoDto productoDto,
            BindingResult result,
            Authentication authentication,
            Model model) throws Exception {

        String email = authentication.getName();
        User usuario = userService.buscarPorEmail(email).orElse(null);

        if (result.hasErrors()) {
            model.addAttribute("categorias", categoriaRepository.findAll());
            model.addAttribute("usuario", usuario);
            return "registerProducto";
        }

        Producto producto = new Producto();
        producto.setNombre(productoDto.getNombre());
        producto.setPrecio(productoDto.getPrecio());
        producto.setDescripcion(productoDto.getDescripcion());
        producto.setEstadoProducto(productoDto.getEstadoProducto());
        producto.setFechaAgregacion(productoDto.getFechaAgregacion());
        producto.setVendedor(usuario);
        producto.setStock(productoDto.getStock());


        if (productoDto.getCategoriaId() != null) {
            Categorias categoria = categoriaRepository.findById(productoDto.getCategoriaId()).orElse(null);
            producto.setCategorias(categoria);
        }

        if (productoDto.getNombreMarca() != null && !productoDto.getNombreMarca().isBlank()) {
            Marca marca = marcaRepository.findByNombreMarca(productoDto.getNombreMarca())
                    .orElseGet(() -> marcaRepository.save(new Marca(productoDto.getNombreMarca())));
            producto.setMarca(marca);
        }

        if (productoDto.getImagenes() != null && !productoDto.getImagenes().isEmpty()) {
            MultipartFile archivo = productoDto.getImagenes().get(0); // primera imagen
            if (!archivo.isEmpty()) {
                String originalFilename = archivo.getOriginalFilename();
                String extension = "";

                int i = originalFilename.lastIndexOf('.');
                if (i >= 0) {
                    extension = originalFilename.substring(i);
                }

                // Generar nombre con el nombre del producto
                String nombreArchivo = productoDto.getNombre()
                        .replaceAll("\\s+", "_")
                        .toLowerCase() + extension;

                Path ruta = Paths.get("C:/uploads/hana");
                if (!Files.exists(ruta)) Files.createDirectories(ruta);

                Files.copy(archivo.getInputStream(), ruta.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);

                producto.setImagen(nombreArchivo);
            }
        }
        if (productoDto.getTallas() != null && !productoDto.getTallas().isEmpty()) {
            List<TallaStock> tallas = productoDto.getTallas().stream()
                    .map(dto -> {
                        TallaStock t = new TallaStock();
                        t.setTalla(dto.getTalla());

                        // ✅ Si el stock viene nulo, lo dejamos en 0
                        Integer stock = dto.getStock() != null ? dto.getStock() : 0;
                        t.setStock(stock);

                        t.setProducto(producto); // MUY IMPORTANTE para la relación bidireccional
                        return t;
                    })
                    .toList();

            producto.setTallas(tallas);
        } else {
            producto.setTallas(null);
        }




        productoRepository.save(producto);
        return "redirect:/vendedor/productos";
    }

    @GetMapping("/productos/desactivar/{id}")
    public String desactivarProducto(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        User usuario = userService.buscarPorEmail(email).orElse(null);

        Producto producto = productoRepository.findById(Integer.valueOf(id.intValue())).orElse(null);

        if (producto != null && producto.getVendedor().equals(usuario)) {
            producto.setActivo(false);
            productoRepository.save(producto);
            logg.info("Producto desactivado: {}", producto.getNombre());
        } else {
            logg.warn("Producto no encontrado o no pertenece al vendedor. ID: {}", id);
        }

        return "redirect:/vendedor/productos";
    }




    @GetMapping("/productos/reporte-pdf")
    public ResponseEntity<ByteArrayResource> generarReportePdf(
            Authentication authentication,
            @RequestParam(value = "keyword", required = false) String keyword) {

        String email = authentication.getName();
        User usuario = userService.buscarPorEmail(email).orElse(null);

        List<Producto> productos;
        if (keyword != null && !keyword.isBlank()) {
            productos = productoService
                    .buscarPorTodosLosCampos(keyword, usuario.getId(), PageRequest.of(0, 1000))
                    .getContent();
        } else {
            productos = productoRepository.findByVendedor(usuario);
        }

        byte[] pdf = pdfThymeleafService.generarPdfDesdeHtml(productos);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=mis_productos.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }


    @GetMapping("/productos/editar/{id}")
    public String mostrarFormularioEdicion(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {

        String email = authentication.getName();
        User usuario = userService.buscarPorEmail(email).orElse(null);

        Producto producto = productoRepository.findById(Integer.valueOf(id.intValue())).orElse(null);
        if (producto == null || !producto.getVendedor().equals(usuario)) {
            return "redirect:/vendedor/productos";
        }

        // Cargar los datos del producto en el DTO
        ProductoDto productoDto = new ProductoDto();
        productoDto.setId((int) producto.getId().longValue());
        productoDto.setNombre(producto.getNombre());
        productoDto.setPrecio(producto.getPrecio());
        productoDto.setDescripcion(producto.getDescripcion());
        productoDto.setEstadoProducto(producto.getEstadoProducto());
        productoDto.setFechaAgregacion(producto.getFechaAgregacion());
        if (producto.getCategorias() != null)
            productoDto.setCategoriaId(producto.getCategorias().getId());
        if (producto.getMarca() != null)
            productoDto.setNombreMarca(producto.getMarca().getNombreMarca());

        if (producto.getTallas() != null && !producto.getTallas().isEmpty()) {
            List<TallasStockDTO> tallasDto = producto.getTallas().stream()
                    .map(t -> {
                        TallasStockDTO dto = new TallasStockDTO();
                        dto.setTalla(t.getTalla());
                        dto.setStock(t.getStock());
                        return dto;
                    })
                    .toList();
            productoDto.setTallas(tallasDto);
        }


        model.addAttribute("productoDto", productoDto);
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("usuario", usuario);

        return "vendedor/edit";
    }

    @GetMapping("/productos/activar/{id}")
    public String activarProducto(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        User usuario = userService.buscarPorEmail(email).orElse(null);

        Producto producto = productoRepository.findById(Integer.valueOf(id.intValue())).orElse(null); // usa intValue() si tu repo usa Integer

        if (producto != null && producto.getVendedor().equals(usuario)) {
            producto.setActivo(true);
            productoRepository.save(producto);
            logg.info("Producto reactivado: {}", producto.getNombre());
        } else {
            logg.warn("Producto no encontrado o no pertenece al vendedor. ID: {}", id);
        }

        return "redirect:/vendedor/productos";
    }


}