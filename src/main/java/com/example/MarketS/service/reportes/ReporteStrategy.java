package com.example.MarketS.service.reportes;

import com.example.MarketS.dto.ReporteAdminDTO;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface ReporteStrategy {
    void exportar(HttpServletResponse response, ReporteAdminDTO reporte) throws IOException;
}
