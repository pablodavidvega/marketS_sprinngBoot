package com.example.MarketS.repository;

import com.example.MarketS.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    List<Resena> findByProductoIdOrderByFechaDesc(Long productoId);

    boolean existsByProductoIdAndCompradorId(Long productoId, Long compradorId);

    long countByProductoId(Long productoId);

    @Query("SELECT COALESCE(AVG(r.calificacion), 0.0) FROM Resena r WHERE r.producto.id = :productoId")
    Double avgCalificacionByProductoId(@Param("productoId") Long productoId);
}
