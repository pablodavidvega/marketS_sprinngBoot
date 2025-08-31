package com.example.MarketS.repository;

import com.example.MarketS.model.Categorias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categorias, Long> {
    Optional<Categorias> findByNombreCategoria(String nombreCategoria);
}
