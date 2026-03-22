package com.example.MarketS.service;

import com.example.MarketS.dto.ReporteAdminDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ReporteAdminServiceTest {

    @Autowired
    private ReporteAdminService reporteAdminService;

    @Test
    void testGenerarReporte() {

        ReporteAdminDTO reporte = reporteAdminService.generarReporte();

        assertNotNull(reporte);
        assertNotNull(reporte.getNombresCategorias());
        assertNotNull(reporte.getTotalesCategorias());

        assertTrue(reporte.getTotalProductos() >= 0);
        assertTrue(reporte.getTotalCategorias() >= 0);

    }
}