package com.example.MarketS.repository;

import com.example.MarketS.model.DetallePedido;
import com.example.MarketS.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {

    // "estados" es siempre ESTADOS_VENTA_CONCRETADA (PAGADO/RECIBIDO/COMPLETADO_EXTERNO, ver
    // VendedorApiController): una venta pagada por MercadoPago pasa de PAGADO a RECIBIDO cuando
    // el cliente confirma recepción (el estado se sobreescribe, no se acumula), así que filtrar
    // solo por 'PAGADO' hacía desaparecer del dashboard/reportes cualquier venta ya confirmada,
    // y de paso dejaba afuera las ventas concretadas por fuera de la app (COMPLETADO_EXTERNO).
    @Query("SELECT COALESCE(SUM(d.gananciaVendedor), 0) FROM DetallePedido d WHERE d.producto.vendedor = :vendedor AND d.pedido.estado IN :estados")
    Double sumGananciaByVendedor(@Param("vendedor") User vendedor, @Param("estados") List<String> estados);

    @Query("SELECT COALESCE(SUM(d.comisionApp), 0) FROM DetallePedido d WHERE d.producto.vendedor = :vendedor AND d.pedido.estado IN :estados")
    Double sumComisionByVendedor(@Param("vendedor") User vendedor, @Param("estados") List<String> estados);

    @Query("SELECT COUNT(d) FROM DetallePedido d WHERE d.producto.vendedor = :vendedor AND d.pedido.estado IN :estados")
    Long countVentasByVendedor(@Param("vendedor") User vendedor, @Param("estados") List<String> estados);

    @Query("SELECT COUNT(d) > 0 FROM DetallePedido d WHERE d.producto.id = :productoId AND d.pedido.usuario.id = :usuarioId AND d.pedido.estado IN :estados")
    boolean haCompradoProducto(@Param("productoId") Long productoId, @Param("usuarioId") Long usuarioId, @Param("estados") List<String> estados);

    @Query("SELECT COALESCE(SUM(d.cantidad), 0) FROM DetallePedido d WHERE d.producto.id = :productoId AND d.pedido.estado IN :estados")
    Long sumCantidadVendidaByProductoId(@Param("productoId") Long productoId, @Param("estados") List<String> estados);

    // Devuelve [productoId, totalVendido] para una lista de IDs — 1 sola query
    @Query("SELECT d.producto.id, COALESCE(SUM(d.cantidad), 0) FROM DetallePedido d WHERE d.pedido.estado IN :estados AND d.producto.id IN :ids GROUP BY d.producto.id")
    List<Object[]> findTotalVendidoByProductoIds(@Param("ids") List<Long> ids, @Param("estados") List<String> estados);

    // Reporte vendedor: ventas agrupadas por producto desde una fecha
    @Query("SELECT d.producto.nombre, COALESCE(SUM(d.cantidad), 0), COALESCE(SUM(d.gananciaVendedor), 0), COALESCE(SUM(d.precioUnitario * d.cantidad), 0) " +
           "FROM DetallePedido d " +
           "WHERE d.producto.vendedor = :vendedor AND d.pedido.estado IN :estados AND d.pedido.fecha >= :desde " +
           "GROUP BY d.producto.id, d.producto.nombre " +
           "ORDER BY COALESCE(SUM(d.gananciaVendedor), 0) DESC")
    List<Object[]> findVentasPorProducto(@Param("vendedor") User vendedor, @Param("estados") List<String> estados, @Param("desde") LocalDateTime desde);

    // Reputación: pedidos DISTINTOS donde el vendedor tiene al menos un detalle, filtrados por
    // un conjunto de estados (p.ej. exitosos = RECIBIDO/COMPLETADO_EXTERNO, fallidos = RECHAZADO_EXTERNO).
    @Query("SELECT COUNT(DISTINCT d.pedido.id) FROM DetallePedido d WHERE d.producto.vendedor.id = :vendedorId AND d.pedido.estado IN :estados")
    long countPedidosDistinctByVendedorAndEstadoIn(@Param("vendedorId") Long vendedorId, @Param("estados") List<String> estados);

    // Reporte vendedor: pedidos DISTINTOS (no detalles ni unidades) dentro de un período, para
    // calcular el valor promedio por pedido — un pedido con 3 unidades del mismo producto cuenta
    // una sola vez, no tres.
    @Query("SELECT COUNT(DISTINCT d.pedido.id) FROM DetallePedido d WHERE d.producto.vendedor = :vendedor AND d.pedido.estado IN :estados AND d.pedido.fecha >= :desde")
    long countPedidosDistinctByVendedor(@Param("vendedor") User vendedor, @Param("estados") List<String> estados, @Param("desde") LocalDateTime desde);

    // Batch: [vendedorId, count] de pedidos distintos en un conjunto de estados, para TODOS los
    // vendedores — 1 sola query en vez de una por vendedor (ver ReputacionService batch).
    @Query("SELECT d.producto.vendedor.id, COUNT(DISTINCT d.pedido.id) FROM DetallePedido d WHERE d.pedido.estado IN :estados GROUP BY d.producto.vendedor.id")
    List<Object[]> countPedidosDistinctAgrupadoPorVendedor(@Param("estados") List<String> estados);

    // Perfil público de vendedor: unidades totales vendidas en un conjunto de estados dado.
    @Query("SELECT COALESCE(SUM(d.cantidad), 0) FROM DetallePedido d WHERE d.producto.vendedor.id = :vendedorId AND d.pedido.estado IN :estados")
    long sumUnidadesByVendedorAndEstadoIn(@Param("vendedorId") Long vendedorId, @Param("estados") List<String> estados);

    // Reporte vendedor: ventas agrupadas por año/mes desde una fecha
    @Query("SELECT EXTRACT(YEAR FROM d.pedido.fecha), EXTRACT(MONTH FROM d.pedido.fecha), COALESCE(SUM(d.gananciaVendedor), 0), COALESCE(SUM(d.cantidad), 0) " +
           "FROM DetallePedido d " +
           "WHERE d.producto.vendedor = :vendedor AND d.pedido.estado IN :estados AND d.pedido.fecha >= :desde " +
           "GROUP BY EXTRACT(YEAR FROM d.pedido.fecha), EXTRACT(MONTH FROM d.pedido.fecha) " +
           "ORDER BY EXTRACT(YEAR FROM d.pedido.fecha), EXTRACT(MONTH FROM d.pedido.fecha)")
    List<Object[]> findVentasPorMes(@Param("vendedor") User vendedor, @Param("estados") List<String> estados, @Param("desde") LocalDateTime desde);
}
