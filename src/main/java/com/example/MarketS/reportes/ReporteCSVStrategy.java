package com.example.MarketS.reportes;

import com.example.MarketS.reportes.ReporteCSVStrategy;
import java.util.List;

public class ReporteCSVStrategy implements ReporteStrategy {

    @Override
    public String generarReporte(List<String> datos) {
        StringBuilder sb = new StringBuilder();

        for (String linea : datos) {
            sb.append(linea).append("\n");
        }

        return sb.toString(); // contenido CSV
    }
}
