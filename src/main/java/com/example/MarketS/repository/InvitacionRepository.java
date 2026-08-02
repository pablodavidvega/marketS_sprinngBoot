package com.example.MarketS.repository;

import com.example.MarketS.model.Invitacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvitacionRepository extends JpaRepository<Invitacion, Long> {
    Optional<Invitacion> findByToken(String token);

    // Todas las invitaciones paginadas, con filtro opcional por el estado derivado que muestra
    // el panel admin (activa/expirada/agotada/revocada — ver InvitacionesPage.tsx#getEstado).
    // countQuery explícito (sin el ORDER BY) para no depender de que Spring Data lo derive bien.
    @Query(
        value = """
            SELECT i FROM Invitacion i
            WHERE (:estado IS NULL
                OR (:estado = 'revocada' AND i.activo = false)
                OR (:estado = 'expirada' AND i.activo = true AND i.fechaExpiracion IS NOT NULL AND i.fechaExpiracion < CURRENT_TIMESTAMP)
                OR (:estado = 'agotada'  AND i.activo = true AND (i.fechaExpiracion IS NULL OR i.fechaExpiracion >= CURRENT_TIMESTAMP) AND i.maxUsos IS NOT NULL AND i.usosActuales >= i.maxUsos)
                OR (:estado = 'activa'   AND i.activo = true AND (i.fechaExpiracion IS NULL OR i.fechaExpiracion >= CURRENT_TIMESTAMP) AND (i.maxUsos IS NULL OR i.usosActuales < i.maxUsos)))
            ORDER BY i.fechaCreacion DESC
            """,
        countQuery = """
            SELECT COUNT(i) FROM Invitacion i
            WHERE (:estado IS NULL
                OR (:estado = 'revocada' AND i.activo = false)
                OR (:estado = 'expirada' AND i.activo = true AND i.fechaExpiracion IS NOT NULL AND i.fechaExpiracion < CURRENT_TIMESTAMP)
                OR (:estado = 'agotada'  AND i.activo = true AND (i.fechaExpiracion IS NULL OR i.fechaExpiracion >= CURRENT_TIMESTAMP) AND i.maxUsos IS NOT NULL AND i.usosActuales >= i.maxUsos)
                OR (:estado = 'activa'   AND i.activo = true AND (i.fechaExpiracion IS NULL OR i.fechaExpiracion >= CURRENT_TIMESTAMP) AND (i.maxUsos IS NULL OR i.usosActuales < i.maxUsos)))
            """
    )
    Page<Invitacion> findAllFiltrado(@Param("estado") String estado, Pageable pageable);

    // Conteos globales para las tarjetas de resumen del panel admin — independientes del
    // filtro/página activos, para que no varíen al paginar o filtrar la tabla.
    @Query("SELECT COUNT(i) FROM Invitacion i WHERE i.activo = false")
    long countRevocadas();

    @Query("SELECT COUNT(i) FROM Invitacion i WHERE i.activo = true AND i.fechaExpiracion IS NOT NULL AND i.fechaExpiracion < CURRENT_TIMESTAMP")
    long countExpiradas();

    @Query("SELECT COUNT(i) FROM Invitacion i WHERE i.activo = true AND (i.fechaExpiracion IS NULL OR i.fechaExpiracion >= CURRENT_TIMESTAMP) AND i.maxUsos IS NOT NULL AND i.usosActuales >= i.maxUsos")
    long countAgotadas();

    @Query("SELECT COUNT(i) FROM Invitacion i WHERE i.activo = true AND (i.fechaExpiracion IS NULL OR i.fechaExpiracion >= CURRENT_TIMESTAMP) AND (i.maxUsos IS NULL OR i.usosActuales < i.maxUsos)")
    long countActivas();

    @Query("SELECT COALESCE(SUM(i.usosActuales), 0) FROM Invitacion i")
    long sumUsosActuales();
}
