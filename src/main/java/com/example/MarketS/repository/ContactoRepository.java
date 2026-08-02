package com.example.MarketS.repository;

import com.example.MarketS.model.Contacto;
import com.example.MarketS.model.EstadoContacto;
import com.example.MarketS.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactoRepository extends JpaRepository<Contacto, Long> {

    // Solicitudes de un usuario, paginadas. Mismo patrón de paginación que
    // ModeradorApiController.pedidos() / PedidoRepository.findAllWithUsuarioFiltrado.
    Page<Contacto> findByUserOrderByFechaCreacionDesc(User user, Pageable pageable);

    // Todas las solicitudes (admin), paginadas, con filtro opcional de estado.
    @Query(
        value = """
            SELECT c FROM Contacto c
            WHERE (:estado IS NULL OR c.estado = :estado)
            ORDER BY c.fechaCreacion DESC
            """,
        countQuery = """
            SELECT COUNT(c) FROM Contacto c
            WHERE (:estado IS NULL OR c.estado = :estado)
            """
    )
    Page<Contacto> findAllFiltrado(@Param("estado") EstadoContacto estado, Pageable pageable);

    long countByEstado(EstadoContacto estado);
}
