package com.example.MarketS.repository;

import com.example.MarketS.model.Notificacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId, Pageable pageable);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.usuario.id = :usuarioId AND n.leida = false")
    void marcarTodasLeidasByUsuarioId(@Param("usuarioId") Long usuarioId);

    // Dedup de recordatorios diarios (RecordatorioPendienteService): ¿ya se generó, desde
    // "desde" (medianoche de hoy), un recordatorio de este tipo para este usuario+asunto?
    // pedidoId/productoId/vendedorId son opcionales según el tipo de recordatorio, de ahí el
    // "(:param IS NULL AND campo IS NULL) OR campo = :param" en vez de una derived query.
    @Query("SELECT COUNT(n) > 0 FROM Notificacion n WHERE n.usuario.id = :usuarioId AND n.tipo = :tipo " +
            "AND ((:pedidoId IS NULL AND n.pedidoId IS NULL) OR n.pedidoId = :pedidoId) " +
            "AND ((:productoId IS NULL AND n.productoId IS NULL) OR n.productoId = :productoId) " +
            "AND ((:vendedorId IS NULL AND n.vendedorId IS NULL) OR n.vendedorId = :vendedorId) " +
            "AND n.fechaCreacion >= :desde")
    boolean existeRecordatorioDesde(@Param("usuarioId") Long usuarioId, @Param("tipo") String tipo,
            @Param("pedidoId") Long pedidoId, @Param("productoId") Long productoId,
            @Param("vendedorId") Long vendedorId, @Param("desde") LocalDateTime desde);
}
