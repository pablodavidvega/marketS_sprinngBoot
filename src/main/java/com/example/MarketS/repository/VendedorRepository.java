package com.example.MarketS.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.MarketS.model.Vendedor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendedorRepository extends JpaRepository<Vendedor,Long> {
    //Filters//
    Optional<Vendedor> findByCity(String city);

    // Batch: [userId, city] para varios vendedores a la vez — evita el N+1 de User.getVendedor()
    // (OneToOne mappedBy sin bytecode enhancement, dispara una consulta extra por usuario si se
    // accede uno por uno; ver PerfilResponseDTO).
    @Query("SELECT v.user.id, v.city FROM Vendedor v WHERE v.user.id IN :userIds")
    List<Object[]> findCityByUserIdIn(@Param("userIds") List<Long> userIds);
}
