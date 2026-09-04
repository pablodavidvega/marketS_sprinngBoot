package com.example.MarketS.repository;

import com.example.MarketS.model.Producto;
import com.example.MarketS.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // Paginado real (usado por HomePage al filtrar por categoría, botón "Ver más"). countQuery
    // explícito, sin los JOIN FETCH, por el mismo motivo que en findAllFiltrado.
    @Query(
        value = """
            SELECT p FROM Producto p
            LEFT JOIN FETCH p.marca
            LEFT JOIN FETCH p.categorias
            WHERE p.categorias.id = :categoriasId AND p.activo = true
            """,
        countQuery = """
            SELECT COUNT(p) FROM Producto p
            WHERE p.categorias.id = :categoriasId AND p.activo = true
            """
    )
    Page<Producto> findByCategoriasIdAndActivoTrue(@Param("categoriasId") Long categoriasId, Pageable pageable);

    @Query("""
        SELECT p FROM Producto p
        LEFT JOIN FETCH p.marca
        LEFT JOIN FETCH p.categorias
        WHERE p.activo = true
    """)
    List<Producto> findByActivoTrue();

    // Top-N productos recientes (usado por el slider de portada, limit chico, sin necesidad de
    // saber el total de páginas).
    @Query("""
        SELECT p FROM Producto p
        LEFT JOIN FETCH p.marca
        LEFT JOIN FETCH p.categorias
        WHERE p.activo = true
        ORDER BY p.fechaAgregacion DESC
    """)
    List<Producto> findByActivoTrueOrderByFechaAgregacionDesc(Pageable pageable);

    // Paginado real (usado por UltimosProductosPage, botón "Ver más"). countQuery explícito,
    // sin los JOIN FETCH, por el mismo motivo que en findAllFiltrado.
    @Query(
        value = """
            SELECT p FROM Producto p
            LEFT JOIN FETCH p.marca
            LEFT JOIN FETCH p.categorias
            WHERE p.activo = true
            ORDER BY p.fechaAgregacion DESC
            """,
        countQuery = "SELECT COUNT(p) FROM Producto p WHERE p.activo = true"
    )
    Page<Producto> paginaByActivoTrueOrderByFechaAgregacionDesc(Pageable pageable);

    List<Producto> findByVendedor(User vendedor);

    List<Producto> findByVendedorIdAndActivoTrue(Long vendedorId);

    // Paginado real (usado por VendedorPerfilPage, botón "Ver más"). countQuery explícito,
    // sin los JOIN FETCH.
    @Query(
        value = """
            SELECT p FROM Producto p
            LEFT JOIN FETCH p.marca
            LEFT JOIN FETCH p.categorias
            WHERE p.vendedor.id = :vendedorId AND p.activo = true
            """,
        countQuery = """
            SELECT COUNT(p) FROM Producto p
            WHERE p.vendedor.id = :vendedorId AND p.activo = true
            """
    )
    Page<Producto> paginaByVendedorIdAndActivoTrue(@Param("vendedorId") Long vendedorId, Pageable pageable);

    Page<Producto> findByVendedorId(Long vendedorId, Pageable pageable);

//    Método para contar productos por categoría
    int countByCategoriasId(Long categoriaId);

    // Conteo por categoría en una sola query (evita el N+1 de llamar countByCategoriasId
    // una vez por cada categoría — ver ReporteAdminService.generarReporte).
    @Query("SELECT p.categorias.id, COUNT(p) FROM Producto p WHERE p.categorias IS NOT NULL GROUP BY p.categorias.id")
    List<Object[]> countProductosGroupByCategoria();

    long countByVendedorId(Long vendedorId);

    // Embudo admin: cuántos vendedores distintos han publicado al menos un producto.
    @Query("SELECT COUNT(DISTINCT p.vendedor.id) FROM Producto p")
    long countDistinctVendedores();

    // Todos los productos (activos e inactivos) paginados, con filtro opcional de
    // activo/inactivo y búsqueda por nombre, vendedor o categoría — para moderador.
    // countQuery explícito (sin los JOIN FETCH, que no aportan nada a un COUNT) para evitar
    // depender de que Spring Data derive correctamente el count a partir de una query con
    // fetch joins.
    @Query(
        value = """
            SELECT p FROM Producto p
            LEFT JOIN FETCH p.marca
            LEFT JOIN FETCH p.categorias c
            LEFT JOIN p.vendedor v
            WHERE (:activo IS NULL OR p.activo = :activo)
              AND (:q IS NULL
                   OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(v.username) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(c.nombreCategoria) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            ORDER BY p.fechaAgregacion DESC
            """,
        countQuery = """
            SELECT COUNT(p) FROM Producto p
            LEFT JOIN p.categorias c
            LEFT JOIN p.vendedor v
            WHERE (:activo IS NULL OR p.activo = :activo)
              AND (:q IS NULL
                   OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(v.username) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(c.nombreCategoria) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            """
    )
    Page<Producto> findAllFiltrado(@Param("activo") Boolean activo, @Param("q") String q, Pageable pageable);

    long countByActivoTrue();
    long countByActivoFalse();

    // UPDATE atómico condicionado (WHERE stock >= cantidad) en vez de leer-chequear-escribir
    // (ver ProductoService.reducirStock). Dos confirmaciones de pago concurrentes para el mismo
    // producto podían pasar ambas el chequeo de stock en memoria antes de que cualquiera
    // guardara, dejando stock negativo (mismo patrón que InvitacionRepository.consumirUso). El
    // motor de base de datos serializa la fila; la segunda ejecución ya no encuentra stock
    // suficiente y no actualiza ninguna fila. clearAutomatically=true limpia el contexto de
    // persistencia para que la relectura posterior en reducirStock traiga el stock real y no el
    // valor cacheado antes del UPDATE.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Producto p SET p.stock = p.stock - :cantidad WHERE p.id = :id AND p.stock >= :cantidad")
    int reducirStockAtomico(@Param("id") Long id, @Param("cantidad") Integer cantidad);
}
