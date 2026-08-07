package com.example.MarketS.repository;

import com.example.MarketS.model.User;
import com.example.MarketS.model.VendedorMercadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendedorMercadoPagoRepository extends JpaRepository<VendedorMercadoPago, Long> {
    Optional<VendedorMercadoPago> findByUser(User user);

    Optional<VendedorMercadoPago> findByUserId(Long userId);

    Optional<VendedorMercadoPago> findByState(String state);
}
