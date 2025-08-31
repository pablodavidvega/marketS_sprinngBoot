package com.example.MarketS.repository;

import com.example.MarketS.model.Producto;
import com.example.MarketS.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {


    List<Producto> findByNombreContainingIgnoreCase(String nombre);


    @Query("""
    SELECT p FROM Producto p
    WHERE p.vendedor.id = :vendedorId AND (
        LOWER(p.nombre) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(p.marca.nombreMarca) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(p.estadoProducto) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(p.categorias.nombreCategoria) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        str(p.precio) LIKE CONCAT('%', :keyword, '%') OR
        str(p.fechaAgregacion) LIKE CONCAT('%', :keyword, '%')
    )
""")
    Page<Producto> buscarPorVendedorYKeyword(
            @Param("vendedorId") Long vendedorId,
            @Param("keyword") String keyword,
            Pageable pageable
    );


    @Query("""
        SELECT p FROM Producto p
        WHERE p.activo = true AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :keyword, '%'))
          AND p.vendedor.id = :vendedorId
    """)
    Page<Producto> buscarPorNombreYVendedorActivos(
            @Param("keyword") String keyword,
            @Param("vendedorId") Long vendedorId,
            Pageable pageable
    );


    List<Producto> findByCategorias_NombreCategoriaIgnoreCase(String nombreCategoria);

    List<Producto> findByCategoriasIdAndActivoTrue(Long categoriasId);

    List<Producto> findByActivoTrue();

    List<Producto> findByVendedor(User vendedor);

    Page<Producto> findByVendedorId(Long vendedorId, Pageable pageable);

}
