package com.example.MarketS.controller.api;

import com.example.MarketS.dto.BannerSlideDTO;
import com.example.MarketS.repository.BannerSlideRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/banners-slider")
public class BannerSlideApiController {

    private final BannerSlideRepository bannerSlideRepository;

    public BannerSlideApiController(BannerSlideRepository bannerSlideRepository) {
        this.bannerSlideRepository = bannerSlideRepository;
    }

    // GET /api/banners-slider/activos — banners activos para el slider del home, ordenados manualmente por el admin
    @GetMapping("/activos")
    public ResponseEntity<List<BannerSlideDTO>> activos() {
        List<BannerSlideDTO> banners = bannerSlideRepository.findByActivoTrueOrderByOrdenAsc()
                .stream()
                .map(BannerSlideDTO::from)
                .toList();
        return ResponseEntity.ok(banners);
    }
}
