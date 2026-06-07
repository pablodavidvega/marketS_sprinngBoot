package com.example.MarketS.repository;

import com.example.MarketS.model.DetallePedido;
import com.example.MarketS.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {

    @Query("SELECT COALESCE(SUM(d.gananciaVendedor), 0) FROM DetallePedido d WHERE d.producto.vendedor = :vendedor AND d.pedido.estado = 'PAGADO'")
    Double sumGananciaByVendedor(@Param("vendedor") User vendedor);

    @Query("SELECT COALESCE(SUM(d.comisionApp), 0) FROM DetallePedido d WHERE d.producto.vendedor = :vendedor AND d.pedido.estado = 'PAGADO'")
    Double sumComisionByVendedor(@Param("vendedor") User vendedor);

    @Query("SELECT COUNT(d) FROM DetallePedido d WHERE d.producto.vendedor = :vendedor AND d.pedido.estado = 'PAGADO'")
    Long countVentasByVendedor(@Param("vendedor") User vendedor);
}
