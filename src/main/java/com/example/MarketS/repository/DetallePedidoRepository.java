package com.example.MarketS.repository;

import com.example.MarketS.model.DetallePedido;
import com.example.MarketS.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {

    @Query("SELECT COALESCE(SUM(d.gananciaVendedor), 0) FROM DetallePedido d WHERE d.producto.vendedor = :vendedor AND d.pedido.estado = 'PAGADO'")
    Double sumGananciaByVendedor(@Param("vendedor") User vendedor);

    @Query("SELECT COALESCE(SUM(d.comisionApp), 0) FROM DetallePedido d WHERE d.producto.vendedor = :vendedor AND d.pedido.estado = 'PAGADO'")
    Double sumComisionByVendedor(@Param("vendedor") User vendedor);

    @Query("SELECT COUNT(d) FROM DetallePedido d WHERE d.producto.vendedor = :vendedor AND d.pedido.estado = 'PAGADO'")
    Long countVentasByVendedor(@Param("vendedor") User vendedor);

    @Query("SELECT COUNT(d) > 0 FROM DetallePedido d WHERE d.producto.id = :productoId AND d.pedido.usuario.id = :usuarioId AND d.pedido.estado = 'PAGADO'")
    boolean haCompradoProducto(@Param("productoId") Long productoId, @Param("usuarioId") Long usuarioId);

    @Query("SELECT COALESCE(SUM(d.cantidad), 0) FROM DetallePedido d WHERE d.producto.id = :productoId AND d.pedido.estado = 'PAGADO'")
    Long sumCantidadVendidaByProductoId(@Param("productoId") Long productoId);

    // Devuelve [productoId, totalVendido] para una lista de IDs — 1 sola query
    @Query("SELECT d.producto.id, COALESCE(SUM(d.cantidad), 0) FROM DetallePedido d WHERE d.pedido.estado = 'PAGADO' AND d.producto.id IN :ids GROUP BY d.producto.id")
    List<Object[]> findTotalVendidoByProductoIds(@Param("ids") List<Long> ids);
}
