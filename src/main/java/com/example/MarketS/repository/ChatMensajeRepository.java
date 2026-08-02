package com.example.MarketS.repository;

import com.example.MarketS.model.ChatMensaje;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMensajeRepository extends JpaRepository<ChatMensaje, Long> {

    // Paginado de 30 en 30, más recientes primero, para que el chat pueda pedir la página 0
    // (últimos mensajes) por defecto y páginas siguientes solo cuando el usuario pide
    // "cargar mensajes anteriores" (ver ChatApiController). Se ordena DESC para que la
    // paginación por offset siempre arranque desde el mensaje más nuevo.
    @Query(value = "SELECT m FROM ChatMensaje m JOIN FETCH m.autor WHERE m.pedido.id = :pedidoId AND m.vendedor.id = :vendedorId ORDER BY m.fechaEnvio DESC",
            countQuery = "SELECT COUNT(m) FROM ChatMensaje m WHERE m.pedido.id = :pedidoId AND m.vendedor.id = :vendedorId")
    Page<ChatMensaje> findByPedidoIdAndVendedorIdOrderByFechaEnvioDesc(@Param("pedidoId") Long pedidoId, @Param("vendedorId") Long vendedorId, Pageable pageable);

    @Modifying
    @Query("UPDATE ChatMensaje m SET m.leido = true WHERE m.pedido.id = :pedidoId AND m.vendedor.id = :vendedorId AND m.autor.id <> :lectorId AND m.leido = false")
    void marcarLeidosParaLector(@Param("pedidoId") Long pedidoId, @Param("vendedorId") Long vendedorId, @Param("lectorId") Long lectorId);

    // Último mensaje de cada hilo (pedido+vendedor) — para RecordatorioPendienteService, que
    // necesita saber quién escribió último en cada chat para recordarle a la contraparte que
    // no respondió. No se hace JOIN FETCH de m.vendedor (mismo motivo que en PedidoRepository:
    // cargar el User completo dispara una consulta extra a "vendedores"); su id se lee del
    // proxy LAZY sin costo.
    @Query("SELECT m FROM ChatMensaje m JOIN FETCH m.autor JOIN FETCH m.pedido p JOIN FETCH p.usuario " +
            "WHERE m.fechaEnvio = (SELECT MAX(m2.fechaEnvio) FROM ChatMensaje m2 " +
            "WHERE m2.pedido.id = m.pedido.id AND m2.vendedor.id = m.vendedor.id)")
    List<ChatMensaje> findUltimoMensajePorHilo();
}
