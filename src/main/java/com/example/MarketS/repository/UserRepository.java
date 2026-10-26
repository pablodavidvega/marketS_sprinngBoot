package com.example.MarketS.repository;

import com.example.MarketS.model.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.MarketS.model.User;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByResetToken(String resetToken);
    Optional<User> findByResetTokenAndResetTokenExpiraAfter(
            String token,
            LocalDateTime fecha);
    List<User> findByAddress(String address);
    List<User> findByRolRolname(String rolname);
    List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email);
    long countByRol(Rol rol);
    long countByRolRolname(String rolname);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // Batch de existencia para carga masiva (ver CargaMasivaUsuariosService): una query por
    // cada username/email de todo el archivo era N+1 sobre potencialmente cientos de filas.
    // Devuelve solo los que YA existen en la BD; el resto de duplicados (dentro del propio
    // archivo) se detecta en memoria.
    @Query("SELECT u.username FROM User u WHERE u.username IN :usernames")
    List<String> findExistingUsernames(@Param("usernames") Collection<String> usernames);

    @Query("SELECT u.email FROM User u WHERE u.email IN :emails")
    List<String> findExistingEmails(@Param("emails") Collection<String> emails);

    // Proyección liviana (solo columnas escalares, no la entidad User completa) para
    // resolver id+username de varios vendedores sin disparar, por cada uno, la consulta
    // extra a "vendedores" que Hibernate hace al cargar un User completo (ver Producto.vendedor).
    @Query("SELECT u.id, u.username FROM User u WHERE u.id IN :ids")
    List<Object[]> findIdAndUsernameByIdIn(@Param("ids") List<Long> ids);

    // Proyección liviana de solo ids por rol — evita cargar la entidad User completa cuando
    // solo hace falta el id (ver AdminApiController.distribucionReputacion).
    @Query("SELECT u.id FROM User u WHERE u.rol.rolname = :rolname")
    List<Long> findIdsByRolRolname(@Param("rolname") String rolname);

    // Proyección liviana para listados de usuarios: selecciona solo columnas escalares, nunca
    // instancia la entidad User completa. Esto es clave — User.vendedor es un OneToOne
    // mappedBy sin bytecode enhancement, así que Hibernate NO puede hacerlo perezoso de verdad:
    // dispara una consulta extra a "vendedores" por cada User que se materializa como entidad,
    // sin importar si el código llama a getVendedor() o no. Una proyección escalar nunca
    // materializa el User, así que ese disparo automático no ocurre (ver AdminApiController.listarUsuarios).
    @Query("SELECT u.id, u.username, u.email, u.phone, u.address, u.rol.rolname, u.createdAt FROM User u")
    List<Object[]> findAllPerfilProjection();

    // Igual que arriba pero paginado, con filtro opcional exacto de rol y búsqueda de texto
    // libre en username/email (la "búsqueda inteligente" del panel admin). Antes
    // AdminApiController.listarUsuarios traía TODOS los usuarios en una sola llamada — con el
    // mismo problema de fondo que PedidoRepository.findAllWithUsuarioFiltrado (ver comentario ahí).
    @Query(
        value = """
            SELECT u.id, u.username, u.email, u.phone, u.address, u.rol.rolname, u.createdAt
            FROM User u
            WHERE (:rol IS NULL OR u.rol.rolname = :rol)
              AND (:q IS NULL
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            """,
        countQuery = """
            SELECT COUNT(u) FROM User u
            WHERE (:rol IS NULL OR u.rol.rolname = :rol)
              AND (:q IS NULL
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            """
    )
    Page<Object[]> findAllPerfilProjectionFiltrado(@Param("rol") String rol, @Param("q") String q, Pageable pageable);

    // Igual que arriba pero con los datos de contacto que necesita el vendedor para coordinar
    // la entrega (ver PedidoVendedorDTO / PedidoRepository.findByVendedorWithDetails).
    @Query("SELECT u.id, u.username, u.email, u.phone, u.address FROM User u WHERE u.id IN :ids")
    List<Object[]> findContactInfoByIdIn(@Param("ids") List<Long> ids);

    @Query(nativeQuery = true, value = """
        SELECT
            u.id,
            u.username,
            u.email,
            COUNT(DISTINCT pr.id)                                                                         AS totalProductos,
            COALESCE(SUM(CASE WHEN ped.estado_pedido = 'PAGADO' THEN d.cantidad          ELSE 0 END), 0) AS totalUnidades,
            COALESCE(SUM(CASE WHEN ped.estado_pedido = 'PAGADO' THEN d.ganancia_vendedor ELSE 0 END), 0) AS gananciaTotal,
            COALESCE(SUM(CASE WHEN ped.estado_pedido = 'PAGADO' THEN d.comision_app      ELSE 0 END), 0) AS comisionTotal,
            COALESCE(SUM(CASE WHEN ped.estado_pedido = 'PAGADO' THEN d.precio_unitario * d.cantidad ELSE 0 END), 0) AS ingresosTotal
        FROM users u
        JOIN roles r ON u.rol_id = r.id
        LEFT JOIN producto pr ON pr.vendedor_id = u.id
        LEFT JOIN detalle_pedido d ON d.id_producto = pr.id
        LEFT JOIN pedidos ped ON ped.id_pedido = d.id_pedido
        WHERE r.rolname = 'ROLE_VENDEDOR'
          AND u.is_active = true
        GROUP BY u.id, u.username, u.email
        ORDER BY gananciaTotal DESC
    """)
    List<Object[]> findEstadisticasTodosVendedores();

    // Embudo admin: promedio de días entre el registro de un vendedor y su primera publicación
    // (solo considera vendedores que ya publicaron al menos un producto).
    @Query(nativeQuery = true, value = """
        SELECT AVG(EXTRACT(EPOCH FROM (pp.primera_publicacion::timestamp - u.created_at)) / 86400.0)
        FROM users u
        JOIN (
            SELECT vendedor_id, MIN(fecha_agregacion) AS primera_publicacion
            FROM producto
            WHERE vendedor_id IS NOT NULL
            GROUP BY vendedor_id
        ) pp ON pp.vendedor_id = u.id
    """)
    Double avgDiasRegistroAPublicacion();

    // Embudo admin: registros de clientes/vendedores agrupados por semana desde una fecha —
    // devuelve [inicioSemana (Date), total (Long)].
    @Query(nativeQuery = true, value = """
        SELECT DATE_TRUNC('week', u.created_at)::date AS semana, COUNT(*) AS total
        FROM users u
        JOIN roles r ON u.rol_id = r.id
        WHERE r.rolname IN ('ROLE_CLIENTE', 'ROLE_VENDEDOR')
          AND u.created_at >= :desde
        GROUP BY semana
        ORDER BY semana
    """)
    List<Object[]> countRegistrosPorSemana(@Param("desde") LocalDateTime desde);

    // Destinatarios del resumen "Novedades del día": filas escalares (id, username, email,
    // tokenBajaNovedades) en vez de entidades, para no disparar la consulta extra a
    // "vendedores" por cada User (ver comentario en User.vendedor).
    @Query("SELECT u.id, u.username, u.email, u.tokenBajaNovedades FROM User u WHERE u.active = true AND u.recibirNovedades = true")
    List<Object[]> findSuscritosNovedades();

    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.tokenBajaNovedades = :token WHERE u.id = :id")
    int asignarTokenBajaNovedades(@Param("id") Long id, @Param("token") String token);

    Optional<User> findByTokenBajaNovedades(String tokenBajaNovedades);
}
