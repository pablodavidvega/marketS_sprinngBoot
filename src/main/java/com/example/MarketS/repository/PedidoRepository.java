package com.example.MarketS.repository;

import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Buscar pedido pendiente de un usuario
    Optional<Pedido> findByUsuarioAndEstado(User usuario, String estado);

    // Método findById ya viene de JpaRepository, pero lo puedes dejar explícito si quieres
    @Override
    Optional<Pedido> findById(Long id);

    // Buscar pedidos por usuario
    List<Pedido> findByUsuario(User usuario);

    // Buscar pedidos por estado
    List<Pedido> findByEstado(String estado);

    // Buscar pedido por ID con detalles cargados (para evitar LazyInitializationException)
    @Query("SELECT p FROM Pedido p LEFT JOIN FETCH p.detalles WHERE p.id = :id")
    Optional<Pedido> findByIdWithDetalles(@Param("id") Long id);
}