package com.example.MarketS.repository;

import com.example.MarketS.model.Pregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {

    List<Pregunta> findByProductoIdOrderByFechaPreguntaDesc(Long productoId);

    long countByProductoId(Long productoId);

    // Preguntas sin responder (para RecordatorioPendienteService). No se hace JOIN FETCH de
    // producto.vendedor (mismo motivo que en PedidoRepository); su id se lee del proxy LAZY.
    @Query("SELECT p FROM Pregunta p JOIN FETCH p.producto WHERE p.respuesta IS NULL")
    List<Pregunta> findPendientesDeRespuesta();
}
