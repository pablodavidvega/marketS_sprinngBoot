package com.example.MarketS.repository;

import com.example.MarketS.model.ChatMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMensajeRepository extends JpaRepository<ChatMensaje, Long> {

    @Query("SELECT m FROM ChatMensaje m JOIN FETCH m.autor WHERE m.pedido.id = :pedidoId AND m.vendedor.id = :vendedorId ORDER BY m.fechaEnvio ASC")
    List<ChatMensaje> findByPedidoIdAndVendedorIdOrderByFechaEnvioAsc(@Param("pedidoId") Long pedidoId, @Param("vendedorId") Long vendedorId);

    @Modifying
    @Query("UPDATE ChatMensaje m SET m.leido = true WHERE m.pedido.id = :pedidoId AND m.vendedor.id = :vendedorId AND m.autor.id <> :lectorId AND m.leido = false")
    void marcarLeidosParaLector(@Param("pedidoId") Long pedidoId, @Param("vendedorId") Long vendedorId, @Param("lectorId") Long lectorId);
}
