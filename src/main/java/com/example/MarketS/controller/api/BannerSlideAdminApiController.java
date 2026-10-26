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

    // POST /api/admin/banners-slider — crear banner (imagen obligatoria, imagenMobile opcional)
    @PostMapping
    public ResponseEntity<?> crear(
            @RequestParam("imagen") MultipartFile imagen,
            @RequestParam(required = false) MultipartFile imagenMobile,
            @RequestParam @NotBlank String texto,
            @RequestParam @NotBlank String enlace,
            @RequestParam(defaultValue = "0") int orden,
            @RequestParam(defaultValue = "true") boolean activo) throws IOException {

        if (imagen.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La imagen es obligatoria"));
        }

        BannerSlide banner = new BannerSlide();
        banner.setImagen(cloudStorageService.subirArchivo(imagen, "banners"));
        if (imagenMobile != null && !imagenMobile.isEmpty()) {
            banner.setImagenMobile(cloudStorageService.subirArchivo(imagenMobile, "banners"));
        }
        banner.setTexto(texto);
        banner.setEnlace(enlace);
        banner.setOrden(orden);
        banner.setActivo(activo);

        BannerSlide guardado = bannerSlideRepository.save(banner);
        return ResponseEntity.ok(BannerSlideDTO.from(guardado));
    }

    // PUT /api/admin/banners-slider/{id} — actualizar (imágenes opcionales, conserva las actuales si
    // no se envían; quitarImagenMobile=true borra la de celular y vuelve a usar la de escritorio)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable Long id,
            @RequestParam(required = false) MultipartFile imagen,
            @RequestParam(required = false) MultipartFile imagenMobile,
            @RequestParam(defaultValue = "false") boolean quitarImagenMobile,
            @RequestParam @NotBlank String texto,
            @RequestParam @NotBlank String enlace,
            @RequestParam(defaultValue = "0") int orden,
            @RequestParam(defaultValue = "true") boolean activo) throws IOException {

        BannerSlide banner = bannerSlideRepository.findById(id).orElse(null);
        if (banner == null) {
            return ResponseEntity.notFound().build();
        }

        String imagenAnterior = banner.getImagen();
        String imagenMobileAnterior = banner.getImagenMobile();
        if (imagen != null && !imagen.isEmpty()) {
            banner.setImagen(cloudStorageService.subirArchivo(imagen, "banners"));
        }
        if (imagenMobile != null && !imagenMobile.isEmpty()) {
            banner.setImagenMobile(cloudStorageService.subirArchivo(imagenMobile, "banners"));
        } else if (quitarImagenMobile) {
            banner.setImagenMobile(null);
        }
        banner.setTexto(texto);
        banner.setEnlace(enlace);
        banner.setOrden(orden);
        banner.setActivo(activo);

        BannerSlide guardado = bannerSlideRepository.save(banner);
        // Después del save: si falla, el banner sigue apuntando a imágenes que todavía existen
        if (imagenAnterior != null && !imagenAnterior.equals(guardado.getImagen())) {
            cloudStorageService.eliminarArchivo(imagenAnterior);
        }
        if (imagenMobileAnterior != null && !imagenMobileAnterior.equals(guardado.getImagenMobile())) {
            cloudStorageService.eliminarArchivo(imagenMobileAnterior);
        }
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
        BannerSlide banner = bannerSlideRepository.findById(id).orElse(null);
        if (banner == null) {
            return ResponseEntity.notFound().build();
        }
        bannerSlideRepository.deleteById(id);
        cloudStorageService.eliminarArchivo(banner.getImagen());
        if (banner.getImagenMobile() != null) {
            cloudStorageService.eliminarArchivo(banner.getImagenMobile());
        }
        return ResponseEntity.ok(Map.of("mensaje", "Banner eliminado correctamente"));
    }
}
