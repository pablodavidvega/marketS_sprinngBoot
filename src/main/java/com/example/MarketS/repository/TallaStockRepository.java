package com.example.MarketS.repository;

import com.example.MarketS.model.TallaStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TallaStockRepository extends JpaRepository<TallaStock, Long> {

    // Buscar tallas por producto
    List<TallaStock> findByProductoId(Long productoId);

    // Buscar una talla específica de un producto
    @Query("SELECT t FROM TallaStock t WHERE t.producto.id = :productoId AND t.talla = :talla")
    Optional<TallaStock> findByProductoIdAndTalla(@Param("productoId") Long productoId,
                                                  @Param("talla") String talla);

    // Sumar el stock total de todas las tallas de un producto
    @Query("SELECT COALESCE(SUM(t.stock), 0) FROM TallaStock t WHERE t.producto.id = :productoId")
    Integer sumStockByProductoId(@Param("productoId") Long productoId);

    // Verificar si existe una talla para un producto
    boolean existsByProductoIdAndTalla(Long productoId, String talla);
}