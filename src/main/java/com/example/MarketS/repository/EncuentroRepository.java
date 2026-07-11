package com.example.MarketS.repository;

import com.example.MarketS.model.Encuentro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EncuentroRepository extends JpaRepository<Encuentro, Long> {

    @Query("SELECT e FROM Encuentro e JOIN FETCH e.propuestoPor WHERE e.pedido.id = :pedidoId AND e.vendedor.id = :vendedorId")
    Optional<Encuentro> findByPedidoIdAndVendedorId(@Param("pedidoId") Long pedidoId, @Param("vendedorId") Long vendedorId);
}
