package com.example.MarketS.repository;

import com.example.MarketS.model.EnlaceSocial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnlaceSocialRepository extends JpaRepository<EnlaceSocial, Long> {
    List<EnlaceSocial> findByVendedorId(Long vendedorId);
}
