package com.example.MarketS.repository;

import com.example.MarketS.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.example.MarketS.model.User;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
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
}
