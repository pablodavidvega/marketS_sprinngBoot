package com.example.MarketS.repository;

import com.example.MarketS.model.ResenaComprador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResenaCompradorRepository extends JpaRepository<ResenaComprador, Long> {

    boolean existsByPedidoIdAndVendedorId(Long pedidoId, Long vendedorId);

    long countByCompradorId(Long compradorId);

    // Usado al validar si un usuario se puede borrar (ver UserServiceImpl.eliminarUsuario):
    // calificaciones que otros vendedores le dejaron a este usuario como comprador.
    boolean existsByVendedorId(Long vendedorId);

    @Query("SELECT COALESCE(AVG(r.calificacion), 0.0) FROM ResenaComprador r WHERE r.comprador.id = :compradorId")
    Double avgCalificacionByCompradorId(@Param("compradorId") Long compradorId);

    // Batch: [compradorId, promedio, total] para TODOS los compradores con al menos una
    // calificación — 1 sola query en vez de una por comprador.
    @Query("SELECT r.comprador.id, AVG(r.calificacion), COUNT(r) FROM ResenaComprador r GROUP BY r.comprador.id")
    List<Object[]> findEstadisticasAgrupadasPorComprador();

    // Pedidos (de la lista dada) que este vendedor ya calificó — 1 sola query para evitar N+1
    // al armar la lista de "pedidos recibidos".
    @Query("SELECT r.pedido.id FROM ResenaComprador r WHERE r.vendedor.id = :vendedorId AND r.pedido.id IN :pedidoIds")
    List<Long> findPedidoIdsCalificados(@Param("vendedorId") Long vendedorId, @Param("pedidoIds") List<Long> pedidoIds);
}
