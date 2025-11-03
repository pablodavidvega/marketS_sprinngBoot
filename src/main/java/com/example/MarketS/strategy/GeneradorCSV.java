package com.example.MarketS.strategy;

import com.example.MarketS.dto.ReporteAdminDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.PrintWriter;

@Component
public class GeneradorCSV implements GeneradorReporteStrategy {

    @Override
    public void generar(ReporteAdminDTO dto, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_admin.csv");

        try (PrintWriter writer = response.getWriter()) {
            writer.println("Total Productos,Total Categorías,Total Marcas,Promedio Precio,Promedio Stock");
            writer.printf("%d,%d,%d,%.2f,%.2f\n",
                    dto.getTotalProductos(),
                    dto.getTotalCategorias(),
                    dto.getTotalMarcas(),
                    dto.getPromedioPrecio(),
                    dto.getPromedioStock());
        }
    }
}
