package com.example.MarketS.repository;

import com.example.MarketS.model.Pregunta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {

    List<Pregunta> findByProductoIdOrderByFechaPreguntaDesc(Long productoId);

    long countByProductoId(Long productoId);
}
