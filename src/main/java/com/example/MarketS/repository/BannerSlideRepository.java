package com.example.MarketS.repository;

import com.example.MarketS.model.BannerSlide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerSlideRepository extends JpaRepository<BannerSlide, Long> {

    List<BannerSlide> findByActivoTrueOrderByOrdenAsc();

    List<BannerSlide> findAllByOrderByOrdenAsc();
}
