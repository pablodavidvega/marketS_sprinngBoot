package com.example.MarketS.controller.api;

import com.example.MarketS.dto.ProductoResponseDTO;
import com.example.MarketS.model.Favorito;
import com.example.MarketS.model.Producto;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.FavoritoRepository;
import com.example.MarketS.repository.ProductoRepository;
import com.example.MarketS.repository.UserRepository;
import com.example.MarketS.security.UsuarioPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/favoritos")
public class FavoritoApiController {

    private final FavoritoRepository favoritoRepository;
    private final ProductoRepository productoRepository;
    private final UserRepository userRepository;

    public FavoritoApiController(FavoritoRepository favoritoRepository,
                                  ProductoRepository productoRepository,
                                  UserRepository userRepository) {
        this.favoritoRepository = favoritoRepository;
        this.productoRepository = productoRepository;
        this.userRepository = userRepository;
    }

    // GET /api/favoritos — productos guardados por el usuario autenticado
    @GetMapping
    public ResponseEntity<?> misFavoritos(Authentication auth) {
        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) return ResponseEntity.status(401).build();

        List<Favorito> favoritos = favoritoRepository.findByUsuarioIdWithProducto(usuario.getId());

        // Vendedor resuelto en batch: producto.getVendedor() es LAZY y, con muchos vendedores
        // distintos, cargar el User completo por producto dispararía la consulta extra a
        // "vendedores" documentada en Producto.vendedor (ver ProductoApiController.buildDTOs).
        List<Long> vendedorIds = favoritos.stream()
                .map(f -> f.getProducto().getVendedor())
                .filter(Objects::nonNull)
                .map(User::getId)
                .distinct()
                .toList();
        Map<Long, String> vendedorUsernames = new HashMap<>();
        if (!vendedorIds.isEmpty()) {
            userRepository.findIdAndUsernameByIdIn(vendedorIds).forEach(row ->
                    vendedorUsernames.put(((Number) row[0]).longValue(), (String) row[1]));
        }

        List<ProductoResponseDTO> productos = favoritos.stream()
                .map(f -> ProductoResponseDTO.from(f.getProducto(), null, null, null, vendedorUsernames))
                .toList();

        return ResponseEntity.ok(productos);
    }

    // GET /api/favoritos/ids — solo los ids de productos favoritos (para marcar el corazón en catálogo/listados)
    @GetMapping("/ids")
    public ResponseEntity<?> misFavoritosIds(Authentication auth) {
        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(favoritoRepository.findProductoIdsByUsuarioId(usuario.getId()));
    }

    // POST /api/favoritos/{productoId} — guardar un producto como favorito
    @PostMapping("/{productoId}")
    public ResponseEntity<?> agregar(@PathVariable Long productoId, Authentication auth) {
        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) return ResponseEntity.status(401).build();

        if (favoritoRepository.existsByProductoIdAndUsuarioId(productoId, usuario.getId())) {
            return ResponseEntity.ok(Map.of("favorito", true));
        }

        Producto producto = productoRepository.findById(productoId).orElse(null);
        if (producto == null) return ResponseEntity.notFound().build();

        Favorito favorito = new Favorito();
        favorito.setProducto(producto);
        favorito.setUsuario(usuario);
        favorito.setFecha(LocalDateTime.now());
        favoritoRepository.save(favorito);

        return ResponseEntity.ok(Map.of("favorito", true));
    }

    // DELETE /api/favoritos/{productoId} — quitar un producto de favoritos
    @DeleteMapping("/{productoId}")
    public ResponseEntity<?> quitar(@PathVariable Long productoId, Authentication auth) {
        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) return ResponseEntity.status(401).build();

        favoritoRepository.findByProductoIdAndUsuarioId(productoId, usuario.getId())
                .ifPresent(favoritoRepository::delete);

        return ResponseEntity.ok(Map.of("favorito", false));
    }
}
