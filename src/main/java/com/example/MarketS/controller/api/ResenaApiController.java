package com.example.MarketS.controller.api;

import com.example.MarketS.model.Producto;
import com.example.MarketS.model.Resena;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.DetallePedidoRepository;
import com.example.MarketS.repository.ProductoRepository;
import com.example.MarketS.repository.ResenaRepository;
import com.example.MarketS.security.UsuarioPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos/{productoId}/resenas")
public class ResenaApiController {

    private final ResenaRepository resenaRepository;
    private final ProductoRepository productoRepository;
    private final DetallePedidoRepository detallePedidoRepository;

    public ResenaApiController(ResenaRepository resenaRepository,
                               ProductoRepository productoRepository,
                               DetallePedidoRepository detallePedidoRepository) {
        this.resenaRepository = resenaRepository;
        this.productoRepository = productoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
    }

    // GET /api/productos/{productoId}/resenas — público
    @GetMapping
    public ResponseEntity<?> listar(@PathVariable Long productoId, Authentication auth) {
        if (productoRepository.findById(productoId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Map<String, Object>> resenas = resenaRepository
                .findByProductoIdOrderByFechaDesc(productoId)
                .stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("calificacion", r.getCalificacion());
                    m.put("comentario", r.getComentario());
                    m.put("compradorUsername", r.getComprador().getUsername());
                    m.put("fecha", r.getFecha().toString());
                    return m;
                })
                .toList();

        double promedio = resenaRepository.avgCalificacionByProductoId(productoId);
        long total = resenaRepository.countByProductoId(productoId);

        boolean yaReseno = false;
        boolean puedeResenar = false;

        if (auth != null && auth.isAuthenticated()) {
            User usuario = UsuarioPrincipal.from(auth);
            if (usuario != null) {
                yaReseno = resenaRepository.existsByProductoIdAndCompradorId(productoId, usuario.getId());
                puedeResenar = !yaReseno && detallePedidoRepository.haCompradoProducto(productoId, usuario.getId());
            }
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("promedio", Math.round(promedio * 10.0) / 10.0);
        respuesta.put("total", total);
        respuesta.put("puedeResenar", puedeResenar);
        respuesta.put("yaReseno", yaReseno);
        respuesta.put("resenas", resenas);

        return ResponseEntity.ok(respuesta);
    }

    // POST /api/productos/{productoId}/resenas — requiere auth y haber comprado
    @PostMapping
    public ResponseEntity<?> crear(@PathVariable Long productoId,
                                   @RequestBody Map<String, Object> body,
                                   Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        User usuario = UsuarioPrincipal.from(auth);
        if (usuario == null) return ResponseEntity.status(401).build();

        Producto producto = productoRepository.findById(productoId).orElse(null);
        if (producto == null) return ResponseEntity.notFound().build();

        if (!detallePedidoRepository.haCompradoProducto(productoId, usuario.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Solo puedes calificar productos que hayas comprado"));
        }

        if (resenaRepository.existsByProductoIdAndCompradorId(productoId, usuario.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ya calificaste este producto"));
        }

        Object calObj = body.get("calificacion");
        if (calObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "La calificación es requerida"));
        }
        int calificacion = Integer.parseInt(calObj.toString());
        if (calificacion < 1 || calificacion > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "La calificación debe ser entre 1 y 5"));
        }

        String comentario = body.get("comentario") instanceof String s ? s.trim() : null;
        if (comentario != null && comentario.isEmpty()) comentario = null;

        Resena resena = new Resena();
        resena.setProducto(producto);
        resena.setComprador(usuario);
        resena.setCalificacion(calificacion);
        resena.setComentario(comentario);
        resena.setFecha(LocalDateTime.now());
        resenaRepository.save(resena);

        return ResponseEntity.ok(Map.of(
                "id", resena.getId(),
                "calificacion", resena.getCalificacion(),
                "comentario", resena.getComentario() != null ? resena.getComentario() : "",
                "compradorUsername", usuario.getUsername(),
                "fecha", resena.getFecha().toString()
        ));
    }
}
