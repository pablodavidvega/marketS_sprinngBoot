package com.example.MarketS.repository;

import com.example.MarketS.model.Marca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MarcaRepository extends JpaRepository<Marca, Long> {
    Optional<Marca> findByNombreMarca(String nombreMarca);
}
