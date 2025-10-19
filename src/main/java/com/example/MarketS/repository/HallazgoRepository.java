package com.example.MarketS.repository;

import com.example.MarketS.model.Hallazgo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HallazgoRepository extends JpaRepository<Hallazgo, Long> {}
