package com.example.MarketS.repository;

import com.example.MarketS.dto.PedidoModeradorDTO;
import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Buscar pedido pendiente de un usuario
    Optional<Pedido> findByUsuarioAndEstado(User usuario, String estado);

    // Método findById ya viene de JpaRepository, pero lo puedes dejar explícito si quieres
    @Override
    Optional<Pedido> findById(Long id);

    // Buscar pedidos por usuario
    List<Pedido> findByUsuario(User usuario);

    // Pedidos del usuario con detalles y productos cargados en una sola query.
    // OJO: no se hace JOIN FETCH de pr.vendedor (el User del vendedor) a propósito — cargar
    // un User completo dispara, por cada uno, una consulta extra a "vendedores" (OneToOne
    // mappedBy sin bytecode enhancement, ver Producto.vendedor), lo que reintroduciría un N+1.
    // El id del vendedor se lee del proxy LAZY sin costo; el username se resuelve aparte en
    // batch (ver PedidoResponseDTO / UserRepository.findIdAndUsernameByIdIn).
    @Query("SELECT DISTINCT p FROM Pedido p JOIN FETCH p.detalles d JOIN FETCH d.producto pr LEFT JOIN FETCH pr.marca LEFT JOIN FETCH pr.categorias WHERE p.usuario = :usuario ORDER BY p.fecha DESC")
    List<Pedido> findByUsuarioWithDetails(@Param("usuario") User usuario);

    // Buscar pedidos por estado
    List<Pedido> findByEstado(String estado);

    // Reputación del comprador: pedidos propios en un conjunto de estados dado.
    long countByUsuarioIdAndEstadoIn(Long usuarioId, List<String> estados);

    // Batch: [usuarioId, count] de pedidos en un conjunto de estados, para TODOS los usuarios —
    // 1 sola query en vez de una por usuario (ver ReputacionService batch).
    @Query("SELECT p.usuario.id, COUNT(p) FROM Pedido p WHERE p.estado IN :estados GROUP BY p.usuario.id")
    List<Object[]> countAgrupadoPorUsuarioYEstadoIn(@Param("estados") List<String> estados);

    // Buscar pedido por ID con detalles cargados (para evitar LazyInitializationException)
    @Query("SELECT p FROM Pedido p LEFT JOIN FETCH p.detalles WHERE p.id = :id")
    Optional<Pedido> findByIdWithDetalles(@Param("id") Long id);

    // Listado paginado de pedidos con filtro opcional de estado/búsqueda, para moderador.
    // Proyecta directamente a PedidoModeradorDTO en vez de devolver la entidad Pedido: esta
    // última tiene detalles FetchType.EAGER (y DetallePedido.producto también EAGER), así que
    // materializarla dispara un N+1 real (una query por pedido para "detalles", y otra más por
    // cada detalle para su "producto") aunque este listado no use ese dato para nada. Antes de
    // esto, con la página ya limitada a 30 filas, esos ~90 queries extra seguían tardando ~6s
    // contra la BD remota; proyectar evita hidratar la entidad por completo (1 query + el count).
    @Query(
        value = """
            SELECT new com.example.MarketS.dto.PedidoModeradorDTO(p.id, u.username, p.fecha, p.estado, p.total)
            FROM Pedido p JOIN p.usuario u
            WHERE (:estado IS NULL OR p.estado = :estado)
              AND (:q IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR str(p.id) LIKE CONCAT('%', CAST(:q AS string), '%'))
            ORDER BY p.fecha DESC
            """,
        countQuery = """
            SELECT COUNT(p) FROM Pedido p JOIN p.usuario u
            WHERE (:estado IS NULL OR p.estado = :estado)
              AND (:q IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR str(p.id) LIKE CONCAT('%', CAST(:q AS string), '%'))
            """
    )
    Page<PedidoModeradorDTO> findAllWithUsuarioFiltrado(@Param("estado") String estado, @Param("q") String q, Pageable pageable);

    // Pedidos que contienen productos del vendedor dado, con detalles cargados.
    // OJO: no se hace JOIN FETCH de pr.vendedor (ya lo conocemos, es el parámetro "vendedor")
    // ni de p.usuario (el comprador) — un pedido puede tener muchos compradores DISTINTOS, y
    // cargar cada User completo dispara la consulta extra a "vendedores" (ver Pedido.usuario).
    // El id del comprador se lee del proxy LAZY sin costo; sus datos de contacto se resuelven
    // aparte en batch (ver PedidoVendedorDTO / UserRepository.findContactInfoByIdIn).
    @Query("SELECT DISTINCT p FROM Pedido p JOIN FETCH p.detalles d JOIN FETCH d.producto pr LEFT JOIN FETCH pr.marca LEFT JOIN FETCH pr.categorias WHERE pr.vendedor = :vendedor ORDER BY p.fecha DESC")
    List<Pedido> findByVendedorWithDetails(@Param("vendedor") User vendedor);

    // Página de IDs de pedidos que contienen productos del vendedor dado — usado por
    // VendedorApiController.pedidosRecibidos (antes traía TODOS los pedidos del vendedor en una
    // sola llamada, mismo problema que findAllWithUsuarioFiltrado). Se pagina SOLO sobre IDs, sin
    // seleccionar el Pedido completo: Pedido.detalles es FetchType.EAGER, así que materializar la
    // entidad dispararía la carga de esa colección para las 30 filas igual (ver comentario en
    // findAllWithUsuarioFiltrado); seleccionar solo el id evita ese costo por completo. Se filtra
    // con EXISTS en vez de JOIN + DISTINCT para no duplicar filas cuando el pedido tiene más de un
    // producto de este vendedor (evita el error de Postgres "ORDER BY expressions must appear in
    // select list" que SELECT DISTINCT + ORDER BY p.fecha dispararía). El detalle completo de esta
    // página se resuelve aparte con findByIdsWithDetalles.
    @Query(
        value = """
            SELECT p.id FROM Pedido p
            WHERE EXISTS (SELECT 1 FROM DetallePedido d WHERE d.pedido = p AND d.producto.vendedor = :vendedor)
            ORDER BY p.fecha DESC
            """,
        countQuery = """
            SELECT COUNT(p) FROM Pedido p
            WHERE EXISTS (SELECT 1 FROM DetallePedido d WHERE d.pedido = p AND d.producto.vendedor = :vendedor)
            """
    )
    Page<Long> findIdsByVendedor(@Param("vendedor") User vendedor, Pageable pageable);

    // Página de IDs de pedidos del comprador dado — usado por PedidoApiController.misPedidos
    // (mismo motivo que findIdsByVendedor: se pagina solo sobre IDs para no materializar
    // Pedido.detalles, EAGER, en toda la lista antes de recortar la página).
    @Query(
        value = "SELECT p.id FROM Pedido p WHERE p.usuario = :usuario ORDER BY p.fecha DESC",
        countQuery = "SELECT COUNT(p) FROM Pedido p WHERE p.usuario = :usuario"
    )
    Page<Long> findIdsByUsuario(@Param("usuario") User usuario, Pageable pageable);

    // Detalle completo (detalles + producto + marca + categorías) de un conjunto puntual de
    // pedidos — mismo JOIN FETCH que findByVendedorWithDetails/findByUsuarioWithDetails pero
    // acotado a los IDs de una sola página (ver findIdsByVendedor/findIdsByUsuario). El orden de
    // :ids no se preserva en la respuesta (la cláusula IN no lo garantiza); quien llama debe
    // reordenar según la página de IDs ya ordenada por fecha.
    @Query("SELECT DISTINCT p FROM Pedido p JOIN FETCH p.detalles d JOIN FETCH d.producto pr LEFT JOIN FETCH pr.marca LEFT JOIN FETCH pr.categorias WHERE p.id IN :ids")
    List<Pedido> findByIdsWithDetalles(@Param("ids") List<Long> ids);

    // Embudo admin: usuarios distintos que compraron al menos una vez en un conjunto de estados.
    @Query("SELECT COUNT(DISTINCT p.usuario.id) FROM Pedido p WHERE p.estado IN :estados")
    long countDistinctUsuarioByEstadoIn(@Param("estados") List<String> estados);

    // Embudo admin: pedidos creados hace más de X tiempo (suficiente para haberse resuelto).
    long countByFechaBefore(LocalDateTime antesDe);

    // Embudo admin: de esos, los que siguen en un estado "sin resolver" (abandono de checkout).
    long countByFechaBeforeAndEstadoIn(LocalDateTime antesDe, List<String> estados);

    // Panel de estadísticas de comunidad (admin): cuántas transacciones se concretaron.
    long countByEstadoIn(List<String> estados);

    // Monto total movido por transacciones concretadas (GMV).
    @Query("SELECT COALESCE(SUM(p.total), 0) FROM Pedido p WHERE p.estado IN :estados")
    double sumTotalByEstadoIn(@Param("estados") List<String> estados);

    // Fecha y monto de cada transacción concretada desde cierta fecha, para agrupar por semana
    // en Java (ver AdminApiController.agruparPorSemana) — evita una query nativa con DATE_TRUNC.
    @Query("SELECT p.fecha, p.total FROM Pedido p WHERE p.estado IN :estados AND p.fecha >= :desde")
    List<Object[]> findFechaYTotalByEstadoInAndFechaAfter(@Param("estados") List<String> estados, @Param("desde") LocalDateTime desde);
}