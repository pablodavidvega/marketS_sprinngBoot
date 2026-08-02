package com.example.MarketS.repository;

import com.example.MarketS.model.Resena;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    // Paginado real (usado por DetallePage, botón "Ver más" de reseñas).
    Page<Resena> findByProductoIdOrderByFechaDesc(Long productoId, Pageable pageable);

    boolean existsByProductoIdAndCompradorId(Long productoId, Long compradorId);

    long countByProductoId(Long productoId);

    @Query("SELECT COALESCE(AVG(r.calificacion), 0.0) FROM Resena r WHERE r.producto.id = :productoId")
    Double avgCalificacionByProductoId(@Param("productoId") Long productoId);

    // Devuelve [productoId, promedio, totalResenas] para una lista de IDs — 1 sola query
    @Query("SELECT r.producto.id, AVG(r.calificacion), COUNT(r) FROM Resena r WHERE r.producto.id IN :ids GROUP BY r.producto.id")
    List<Object[]> findEstadisticasByProductoIds(@Param("ids") List<Long> ids);

    // Reputación agregada del vendedor: promedio/total de las reseñas de TODOS sus productos.
    @Query("SELECT COALESCE(AVG(r.calificacion), 0.0) FROM Resena r WHERE r.producto.vendedor.id = :vendedorId")
    Double avgCalificacionByVendedorId(@Param("vendedorId") Long vendedorId);

    long countByProductoVendedorId(Long vendedorId);

    // Batch: [vendedorId, promedio, total] para TODOS los vendedores con al menos una reseña —
    // 1 sola query en vez de una por vendedor (ver AdminApiController.distribucionReputacion).
    @Query("SELECT r.producto.vendedor.id, AVG(r.calificacion), COUNT(r) FROM Resena r GROUP BY r.producto.vendedor.id")
    List<Object[]> findEstadisticasAgrupadasPorVendedor();
}
