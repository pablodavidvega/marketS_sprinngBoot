package com.example.MarketS.repository;

import com.example.MarketS.model.Contacto;
import com.example.MarketS.model.EstadoContacto;
import com.example.MarketS.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactoRepository extends JpaRepository<Contacto, Long> {
    List<Contacto> findByUserOrderByFechaCreacionDesc(User user);
    List<Contacto> findAllByOrderByFechaCreacionDesc();
    long countByEstado(EstadoContacto estado);
}
