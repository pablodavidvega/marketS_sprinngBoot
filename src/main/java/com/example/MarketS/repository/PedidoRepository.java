package com.example.MarketS.repository;

import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Buscar pedido pendiente de un usuario
    Optional<Pedido> findByUsuarioAndEstado(User usuario, String estado);
}
