package com.example.MarketS.controller;

import com.example.MarketS.dto.ReporteAdminDTO;
import com.example.MarketS.service.reportes.ReporteContexto;
import com.example.MarketS.service.reportes.ReportePdfStrategy;
import com.example.MarketS.service.reportes.ReporteExcelStrategy;
import com.example.MarketS.service.reportes.ReporteCsvStrategy;
import com.example.MarketS.service.ReporteAdminService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.io.IOException;

@Controller
public class ReporteAdminController {

    @Autowired
    private ReporteContexto reporteContexto;

    @Autowired
    private ReporteAdminService reporteAdminService;

    @GetMapping("/admin/reporte-pdf")
    public String mostrarEstadisticas(Model model) {
        // Generar el reporte con datos reales
        ReporteAdminDTO reporte = reporteAdminService.generarReporte();

        // Enviar datos al HTML
        model.addAttribute("reporte", reporte);
        model.addAttribute("categoriasNombres", reporte.getNombresCategorias());
        model.addAttribute("categoriasTotales", reporte.getTotalesCategorias());

        return "reporte-pdf-admin/estadisticas-admin";
    }

    @GetMapping("/admin/reporte-pdf/descargar")
    public void descargarPdf(HttpServletResponse response) throws IOException {
        ReporteAdminDTO reporte = reporteAdminService.generarReporte();
        reporteContexto.setEstrategia(new ReportePdfStrategy());
        reporteContexto.exportar(response, reporte);
    }

    @GetMapping("/admin/reporte-excel/descargar")
    public void descargarExcel(HttpServletResponse response) throws IOException {
        ReporteAdminDTO reporte = reporteAdminService.generarReporte();
        reporteContexto.setEstrategia(new ReporteExcelStrategy());
        reporteContexto.exportar(response, reporte);
    }

    @GetMapping("/admin/reporte-csv/descargar")
    public void descargarCsv(HttpServletResponse response) throws IOException {
        ReporteAdminDTO reporte = reporteAdminService.generarReporte();
        reporteContexto.setEstrategia(new ReporteCsvStrategy());
        reporteContexto.exportar(response, reporte);
    }

}
