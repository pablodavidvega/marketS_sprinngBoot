package com.example.MarketS.repository;

import com.example.MarketS.model.ResolucionVentaExterna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResolucionVentaExternaRepository extends JpaRepository<ResolucionVentaExterna, Long> {

    boolean existsByPedidoIdAndVendedorId(Long pedidoId, Long vendedorId);

    Optional<ResolucionVentaExterna> findByPedidoIdAndVendedorId(Long pedidoId, Long vendedorId);

    List<ResolucionVentaExterna> findByPedidoId(Long pedidoId);

    // Pedidos (de la lista dada) donde este vendedor YA resolvió su parte — para que el
    // frontend oculte los botones "compra realizada/no se realizó" en su propia vista aunque
    // el pedido siga en ACORDADO_EXTERNO porque otro vendedor del mismo pedido no resolvió aún.
    @Query("SELECT r.pedido.id FROM ResolucionVentaExterna r WHERE r.vendedor.id = :vendedorId AND r.pedido.id IN :pedidoIds")
    List<Long> findPedidoIdsResueltosPorVendedor(@Param("vendedorId") Long vendedorId, @Param("pedidoIds") List<Long> pedidoIds);

    // Pares (pedidoId, vendedorId) ya resueltos entre los pedidos dados — para
    // RecordatorioPendienteService, que necesita saber qué vendedores de un pedido
    // ACORDADO_EXTERNO todavía no confirmaron/rechazaron su parte. Una sola query batch en vez
    // de un existsByPedidoIdAndVendedorId por cada (pedido, vendedor) del lote.
    @Query("SELECT r.pedido.id, r.vendedor.id FROM ResolucionVentaExterna r WHERE r.pedido.id IN :pedidoIds")
    List<Object[]> findParesPedidoVendedorResueltos(@Param("pedidoIds") List<Long> pedidoIds);
}
