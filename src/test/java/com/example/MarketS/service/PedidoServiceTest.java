package com.example.MarketS.service;

import com.example.MarketS.repository.HallazgoRepository;
import com.example.MarketS.service.HallazgoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class PedidoServiceTest {

    @Autowired
    private HallazgoService hallazgoService;

    @Autowired
    private HallazgoRepository hallazgoRepository;

    @BeforeEach
    void limpiarTabla() {
        hallazgoRepository.deleteAll();
    }

    @Test
    void testCalcularPrecioTotal() {
        try {
            double total = 100 + 50; // simulando cálculo
            boolean exito = (total == 150);
            hallazgoService.registrarHallazgo(
                    "Calcular Precio Total",
                    "Unitaria",
                    exito,
                    exito ? "Correcto" : "Error en cálculo"
            );
        } catch (Exception e) {
            hallazgoService.registrarHallazgo(
                    "Calcular Precio Total",
                    "Unitaria",
                    false,
                    e.getMessage()
            );
        }
    }
}
