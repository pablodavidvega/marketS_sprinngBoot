package com.example.MarketS.repository;

import com.example.MarketS.model.Favorito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    boolean existsByProductoIdAndUsuarioId(Long productoId, Long usuarioId);

    Optional<Favorito> findByProductoIdAndUsuarioId(Long productoId, Long usuarioId);

    // Con JOIN FETCH de marca/categorias: evita N+1 al listar "mis favoritos" (mismo patrón
    // que ProductoRepository). El vendedor se resuelve aparte en batch en el controller.
    // countQuery explícito (sin los JOIN FETCH, que no aportan nada a un COUNT) — mismo patrón
    // que ProductoRepository.findAllFiltrado.
    @Query(
        value = """
            SELECT f FROM Favorito f
            JOIN FETCH f.producto p
            LEFT JOIN FETCH p.marca
            LEFT JOIN FETCH p.categorias
            WHERE f.usuario.id = :usuarioId
            ORDER BY f.fecha DESC
            """,
        countQuery = """
            SELECT COUNT(f) FROM Favorito f
            WHERE f.usuario.id = :usuarioId
            """
    )
    Page<Favorito> findByUsuarioIdWithProducto(@Param("usuarioId") Long usuarioId, Pageable pageable);

    @Query("SELECT f.producto.id FROM Favorito f WHERE f.usuario.id = :usuarioId")
    List<Long> findProductoIdsByUsuarioId(@Param("usuarioId") Long usuarioId);

    // Producto(s) con más favoritos guardados — [productoId, nombre, totalFavoritos],
    // usado en el dashboard de admin/moderador (limitar con Pageable, ej. PageRequest.of(0, 1)).
    @Query("""
        SELECT f.producto.id, f.producto.nombre, COUNT(f)
        FROM Favorito f
        GROUP BY f.producto.id, f.producto.nombre
        ORDER BY COUNT(f) DESC
    """)
    List<Object[]> findProductosMasGuardados(Pageable pageable);
}
