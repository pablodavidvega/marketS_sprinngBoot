package com.example.MarketS.service.reportes;

import com.example.MarketS.dto.ReporteAdminDTO;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class ReporteCsvStrategy implements ReporteStrategy {

    @Override
    public void exportar(HttpServletResponse response, ReporteAdminDTO reporte) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=reporte.csv");

        PrintWriter writer = response.getWriter();
        writer.println("Total Productos,Total Categorías,Total Marcas,Promedio Precio,Promedio Stock");
        writer.println(reporte.getTotalProductos() + "," +
                reporte.getTotalCategorias() + "," +
                reporte.getTotalMarcas() + "," +
                reporte.getPromedioPrecio() + "," +
                reporte.getPromedioStock());
        writer.flush();
    }
}
