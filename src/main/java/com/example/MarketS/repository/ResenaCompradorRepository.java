package com.example.MarketS.repository;

import com.example.MarketS.model.ResenaComprador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResenaCompradorRepository extends JpaRepository<ResenaComprador, Long> {

    boolean existsByPedidoIdAndVendedorId(Long pedidoId, Long vendedorId);

    long countByCompradorId(Long compradorId);

    @Query("SELECT COALESCE(AVG(r.calificacion), 0.0) FROM ResenaComprador r WHERE r.comprador.id = :compradorId")
    Double avgCalificacionByCompradorId(@Param("compradorId") Long compradorId);

    // Pedidos (de la lista dada) que este vendedor ya calificó — 1 sola query para evitar N+1
    // al armar la lista de "pedidos recibidos".
    @Query("SELECT r.pedido.id FROM ResenaComprador r WHERE r.vendedor.id = :vendedorId AND r.pedido.id IN :pedidoIds")
    List<Long> findPedidoIdsCalificados(@Param("vendedorId") Long vendedorId, @Param("pedidoIds") List<Long> pedidoIds);
}
