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
public interface ProductoRepository extends JpaRepository<Producto, Long> {


    // Con JOIN FETCH: evita N+1 sobre marca/categorias (ambas @ManyToOne EAGER que, sin
    // fetch join, Hibernate resuelve con un SELECT separado por cada producto). El vendedor
    // se resuelve aparte en batch (ver ProductoApiController.buildDTOs) para no cargar el
    // User completo por producto.
    @Query("""
        SELECT p FROM Producto p
        LEFT JOIN FETCH p.marca
        LEFT JOIN FETCH p.categorias
        WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))
    """)
    List<Producto> findByNombreContainingIgnoreCase(@Param("nombre") String nombre);


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

    @Query("""
        SELECT p FROM Producto p
        LEFT JOIN FETCH p.marca
        LEFT JOIN FETCH p.categorias
        WHERE p.categorias.id = :categoriasId AND p.activo = true
    """)
    List<Producto> findByCategoriasIdAndActivoTrue(@Param("categoriasId") Long categoriasId);

    @Query("""
        SELECT p FROM Producto p
        LEFT JOIN FETCH p.marca
        LEFT JOIN FETCH p.categorias
        WHERE p.activo = true
    """)
    List<Producto> findByActivoTrue();

    @Query("""
        SELECT p FROM Producto p
        LEFT JOIN FETCH p.marca
        LEFT JOIN FETCH p.categorias
        WHERE p.activo = true
        ORDER BY p.fechaAgregacion DESC
    """)
    List<Producto> findByActivoTrueOrderByFechaAgregacionDesc(Pageable pageable);

    @Query("""
        SELECT p FROM Producto p
        LEFT JOIN FETCH p.marca
        LEFT JOIN FETCH p.categorias
        WHERE p.activo = true
        ORDER BY p.fechaAgregacion DESC
    """)
    List<Producto> findByActivoTrueOrderByFechaAgregacionDesc();

    List<Producto> findByVendedor(User vendedor);

    List<Producto> findByVendedorIdAndActivoTrue(Long vendedorId);

    Page<Producto> findByVendedorId(Long vendedorId, Pageable pageable);

//    Método para contar productos por categoría
    int countByCategoriasId(Long categoriaId);

    long countByVendedorId(Long vendedorId);

    // Embudo admin: cuántos vendedores distintos han publicado al menos un producto.
    @Query("SELECT COUNT(DISTINCT p.vendedor.id) FROM Producto p")
    long countDistinctVendedores();
}
