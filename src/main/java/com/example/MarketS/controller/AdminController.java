package com.example.MarketS.controller;

import com.example.MarketS.model.User;
import com.example.MarketS.repository.RolRepository;
import com.example.MarketS.service.ReporteEstadisticoService;
import com.example.MarketS.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final RolRepository rolRepository;
    private final ReporteEstadisticoService reporteEstadisticoService;

    @Autowired
    public AdminController(UserService userService, RolRepository rolRepository, ReporteEstadisticoService reporteEstadisticoService) {
        this.userService = userService;
        this.rolRepository = rolRepository;
        this.reporteEstadisticoService = reporteEstadisticoService;
    }

    // Dashboard principal
    @GetMapping("/dashboard")
    public String mostrarDashboardAdmin(Authentication authentication, Model model) {
        String email = authentication.getName();
        User admin = userService.buscarPorEmail(email).orElse(null);

        model.addAttribute("admin", admin);
        model.addAttribute("activePage", "dashboard");

        // Datos de estadísticas
        model.addAttribute("totalUsuarios", userService.listarTodos().size());
        model.addAttribute("totalClientes", userService.listarPorRol("ROLE_CLIENTE").size());
        model.addAttribute("totalVendedores", userService.listarPorRol("ROLE_VENDEDOR").size());

        return "admin/dashboard";
    }

    // Página de envío de correos
    @GetMapping("/enviar-correos")
    public String mostrarFormularioCorreos(Model model) {
        model.addAttribute("activePage", "enviarCorreos");
        return "admin/enviarCorreos";
    }

    // Descargar reporte estadístico en PDF
    @GetMapping("/reporte-estadistico")
    public ResponseEntity<byte[]> generarReporteEstadistico() {
        try {
            // El servicio ya devuelve un arreglo de bytes
            byte[] pdfBytes = reporteEstadisticoService.generarReporteEstadistico();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "reporte_estadistico.pdf");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Reporte de vendedores
    @GetMapping("/reporte-vendedores")
    public String mostrarReporteVendedores(Model model) {
        model.addAttribute("activePage", "reporteVendedores");
        // Aquí podrías agregar más datos si la vista los necesita
        return "admin/reporteVendedores"; // nombre del HTML Thymeleaf
    }



}
