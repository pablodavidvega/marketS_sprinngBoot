package com.example.MarketS.controller;

import com.example.MarketS.service.HallazgoService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

//Archivo que contiene el informe de los hallazgos de las pruebas realizadas en la aplicación
@Controller
public class HallazgoController {

    @Autowired
    private HallazgoService hallazgoService;

    @GetMapping("/admin/reporte-hallazgos")
    public String mostrarReporte(Model model) {
        model.addAttribute("hallazgos", hallazgoService.obtenerTodos());
        return "reporte-hallazgos";
    }

    @GetMapping("/admin/reporte-hallazgos/pdf")
    public void generarPdf(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"reporte-hallazgos.pdf\"");

        // Lógica para generar PDF con los hallazgos
        hallazgoService.exportarPdf(response.getOutputStream());
    }

}
