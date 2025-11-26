package com.example.MarketS.carga.controller;

import com.example.MarketS.carga.dto.DTOResultadoCarga;
import com.example.MarketS.carga.service.CargaArchivoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
public class CargaProductoController {

    private final CargaArchivoService cargaArchivoService;

    // Mostrar formulario
    @GetMapping("/admin/carga-productos/form")
    public String mostrarFormulario() {
        return "admin/carga_productos_form";
    }

    // Procesar archivo CSV o Excel
    @PostMapping("/admin/carga-productos/subir")
    public String subirArchivo(@RequestParam("archivo") MultipartFile file, Model model) {

        if (file.isEmpty()) {
            model.addAttribute("mensaje", "Debes seleccionar un archivo.");
            return "admin/carga_productos_form";
        }

        DTOResultadoCarga resultado = cargaArchivoService.procesarArchivo(file);

        model.addAttribute("mensaje",
                "Registros procesados: " + resultado.getProcesados() +
                        " | Éxitos: " + resultado.getProcesados() +
                        " | Errores: " + resultado.getFallidos()
        );

        model.addAttribute("errores", resultado.getErrores());

        return "admin/carga_productos_form";
    }
}
