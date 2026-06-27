package com.example.MarketS.controller.api;

import com.example.MarketS.dto.BannerSlideDTO;
import com.example.MarketS.model.BannerSlide;
import com.example.MarketS.repository.BannerSlideRepository;
import com.example.MarketS.service.CloudStorageService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/admin/banners-slider")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class BannerSlideAdminApiController {

    private final BannerSlideRepository bannerSlideRepository;
    private final CloudStorageService cloudStorageService;

    public BannerSlideAdminApiController(BannerSlideRepository bannerSlideRepository,
                                          CloudStorageService cloudStorageService) {
        this.bannerSlideRepository = bannerSlideRepository;
        this.cloudStorageService = cloudStorageService;
    }

    // GET /api/admin/banners-slider — listado completo (incluye inactivos) para el panel CMS
    @GetMapping
    public ResponseEntity<List<BannerSlideDTO>> listar() {
        List<BannerSlideDTO> banners = bannerSlideRepository.findAllByOrderByOrdenAsc()
                .stream()
                .map(BannerSlideDTO::from)
                .toList();
        return ResponseEntity.ok(banners);
    }

    // POST /api/admin/banners-slider — crear banner (imagen obligatoria)
    @PostMapping
    public ResponseEntity<?> crear(
            @RequestParam("imagen") MultipartFile imagen,
            @RequestParam @NotBlank String texto,
            @RequestParam @NotBlank String enlace,
            @RequestParam(defaultValue = "0") int orden,
            @RequestParam(defaultValue = "true") boolean activo) throws IOException {

        if (imagen.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La imagen es obligatoria"));
        }

        BannerSlide banner = new BannerSlide();
        banner.setImagen(cloudStorageService.subirArchivo(imagen, "banners"));
        banner.setTexto(texto);
        banner.setEnlace(enlace);
        banner.setOrden(orden);
        banner.setActivo(activo);

        BannerSlide guardado = bannerSlideRepository.save(banner);
        return ResponseEntity.ok(BannerSlideDTO.from(guardado));
    }

    // PUT /api/admin/banners-slider/{id} — actualizar (imagen opcional, conserva la actual si no se envía)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable Long id,
            @RequestParam(required = false) MultipartFile imagen,
            @RequestParam @NotBlank String texto,
            @RequestParam @NotBlank String enlace,
            @RequestParam(defaultValue = "0") int orden,
            @RequestParam(defaultValue = "true") boolean activo) throws IOException {

        BannerSlide banner = bannerSlideRepository.findById(id).orElse(null);
        if (banner == null) {
            return ResponseEntity.notFound().build();
        }

        if (imagen != null && !imagen.isEmpty()) {
            banner.setImagen(cloudStorageService.subirArchivo(imagen, "banners"));
        }
        banner.setTexto(texto);
        banner.setEnlace(enlace);
        banner.setOrden(orden);
        banner.setActivo(activo);

        BannerSlide guardado = bannerSlideRepository.save(banner);
        return ResponseEntity.ok(BannerSlideDTO.from(guardado));
    }

    // PATCH /api/admin/banners-slider/{id}/activar
    @PatchMapping("/{id}/activar")
    public ResponseEntity<?> activar(@PathVariable Long id) {
        return cambiarActivo(id, true);
    }

    // PATCH /api/admin/banners-slider/{id}/desactivar
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable Long id) {
        return cambiarActivo(id, false);
    }

    private ResponseEntity<?> cambiarActivo(Long id, boolean activo) {
        BannerSlide banner = bannerSlideRepository.findById(id).orElse(null);
        if (banner == null) {
            return ResponseEntity.notFound().build();
        }
        banner.setActivo(activo);
        return ResponseEntity.ok(BannerSlideDTO.from(bannerSlideRepository.save(banner)));
    }

    // DELETE /api/admin/banners-slider/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (bannerSlideRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        bannerSlideRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("mensaje", "Banner eliminado correctamente"));
    }
}
