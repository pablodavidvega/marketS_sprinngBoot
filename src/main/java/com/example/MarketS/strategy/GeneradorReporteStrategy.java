package com.example.MarketS.strategy;

import com.example.MarketS.dto.ReporteAdminDTO;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface GeneradorReporteStrategy {
    void generar(ReporteAdminDTO dto, HttpServletResponse response) throws IOException;
}
