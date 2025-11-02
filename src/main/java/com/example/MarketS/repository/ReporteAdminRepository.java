package com.example.MarketS.repository;

import com.example.MarketS.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public interface ReporteAdminRepository extends JpaRepository<Producto, Long> {

    @Query("""
        SELECT 
            COUNT(p.id) AS totalProductos,
            COUNT(DISTINCT c.id) AS totalCategorias,
            COUNT(DISTINCT p.marca.id) AS totalMarcas,
            AVG(p.precio) AS promedioPrecio,
            AVG(p.stock) AS promedioStock
        FROM Producto p
        LEFT JOIN p.categorias c
    """)
    Map<String, Object> obtenerEstadisticasGlobales();
}
