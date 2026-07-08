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

    // Pedidos del usuario con detalles y productos cargados en una sola query.
    // OJO: no se hace JOIN FETCH de pr.vendedor (el User del vendedor) a propósito — cargar
    // un User completo dispara, por cada uno, una consulta extra a "vendedores" (OneToOne
    // mappedBy sin bytecode enhancement, ver Producto.vendedor), lo que reintroduciría un N+1.
    // El id del vendedor se lee del proxy LAZY sin costo; el username se resuelve aparte en
    // batch (ver PedidoResponseDTO / UserRepository.findIdAndUsernameByIdIn).
    @Query("SELECT DISTINCT p FROM Pedido p JOIN FETCH p.detalles d JOIN FETCH d.producto pr LEFT JOIN FETCH pr.marca LEFT JOIN FETCH pr.categorias WHERE p.usuario = :usuario ORDER BY p.fecha DESC")
    List<Pedido> findByUsuarioWithDetails(@Param("usuario") User usuario);

    // Buscar pedidos por estado
    List<Pedido> findByEstado(String estado);

    // Reputación del comprador: pedidos propios en un conjunto de estados dado.
    long countByUsuarioIdAndEstadoIn(Long usuarioId, List<String> estados);

    // Buscar pedido por ID con detalles cargados (para evitar LazyInitializationException)
    @Query("SELECT p FROM Pedido p LEFT JOIN FETCH p.detalles WHERE p.id = :id")
    Optional<Pedido> findByIdWithDetalles(@Param("id") Long id);

    // Todos los pedidos con comprador cargado (para moderador)
    @Query("SELECT p FROM Pedido p JOIN FETCH p.usuario ORDER BY p.fecha DESC")
    List<Pedido> findAllWithUsuario();

    // Pedidos que contienen productos del vendedor dado, con detalles cargados.
    // OJO: no se hace JOIN FETCH de pr.vendedor (ya lo conocemos, es el parámetro "vendedor")
    // ni de p.usuario (el comprador) — un pedido puede tener muchos compradores DISTINTOS, y
    // cargar cada User completo dispara la consulta extra a "vendedores" (ver Pedido.usuario).
    // El id del comprador se lee del proxy LAZY sin costo; sus datos de contacto se resuelven
    // aparte en batch (ver PedidoVendedorDTO / UserRepository.findContactInfoByIdIn).
    @Query("SELECT DISTINCT p FROM Pedido p JOIN FETCH p.detalles d JOIN FETCH d.producto pr LEFT JOIN FETCH pr.marca LEFT JOIN FETCH pr.categorias WHERE pr.vendedor = :vendedor ORDER BY p.fecha DESC")
    List<Pedido> findByVendedorWithDetails(@Param("vendedor") User vendedor);
}