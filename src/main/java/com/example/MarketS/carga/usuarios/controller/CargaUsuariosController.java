package com.example.MarketS.carga.usuarios.controller;

import com.example.MarketS.carga.usuarios.dto.DTOResultadoCargaUsuarios;
import com.example.MarketS.carga.usuarios.service.CargaUsuariosService;
import com.example.MarketS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
public class CargaUsuariosController {

    private final CargaUsuariosService cargaUsuariosService;
    private final UserRepository userRepository;

    @GetMapping("/admin/carga-usuarios/form")
    public String mostrarFormulario() {
        return "admin/carga_usuarios_form";
    }

    @PostMapping("/admin/carga-usuarios/subir")
    public String subir(@RequestParam("archivo") MultipartFile file, Model model) {

        if (file.isEmpty()) {
            model.addAttribute("mensaje", "Debes seleccionar un archivo.");
            return "admin/carga_usuarios_form";
        }

        DTOResultadoCargaUsuarios r = cargaUsuariosService.procesarArchivo(file);

        model.addAttribute("mensaje",
                "Procesados: " + r.getProcesados() +
                        " | Errores: " + r.getFallidos()
        );

        model.addAttribute("errores", r.getErrores());

        return "admin/carga_usuarios_form";
    }


}