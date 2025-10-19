package com.example.MarketS.controller;

import com.example.MarketS.service.HallazgoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HallazgoController {

    @Autowired
    private HallazgoService hallazgoService;

    @GetMapping("/admin/reporte-hallazgos")
    public String mostrarReporte(Model model) {
        model.addAttribute("hallazgos", hallazgoService.obtenerTodos());
        return "reporte-hallazgos";
    }
}
