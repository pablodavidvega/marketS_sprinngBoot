package com.example.MarketS.service.reportes;

import com.example.MarketS.dto.ReporteAdminDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
public class ReporteContexto {

    private ReporteStrategy estrategia;

    public void setEstrategia(ReporteStrategy estrategia) {
        this.estrategia = estrategia;
    }

    public void exportar(HttpServletResponse response, ReporteAdminDTO reporte) throws IOException {
        if (estrategia == null) {
            throw new IllegalStateException("No se ha definido una estrategia de exportación");
        }
        estrategia.exportar(response, reporte);
    }
}
