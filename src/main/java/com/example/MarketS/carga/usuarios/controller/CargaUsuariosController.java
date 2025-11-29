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

import com.example.MarketS.model.User; // Para el método todosUsuarios
import java.util.List; // Para usar List

@Controller
@RequiredArgsConstructor
public class CargaUsuariosController {

    private final CargaUsuariosService cargaUsuariosService;
    private final UserRepository userRepository; // Añade esta línea

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

    // 🔍 MÉTODO DE DIAGNÓSTICO - Agrega esto
    @GetMapping("/admin/verificar-usuarios")
    @ResponseBody
    public String verificarUsuarios() {
        StringBuilder resultado = new StringBuilder();

        // Verificar usuarios específicos
        String[] usuarios = {"juanperez", "mariaruiz", "carlosgomez", "lauradiaz", "pedrolopez"};

        for (String username : usuarios) {
            boolean existe = userRepository.existsByUsername(username);
            resultado.append("Usuario '").append(username).append("': ").append(existe ? "EXISTE" : "NO EXISTE").append("<br>");
        }

        return resultado.toString();
    }

    // 🎯 MÉTODO PARA VER TODOS LOS USUARIOS - También útil
    @GetMapping("/admin/todos-usuarios")
    @ResponseBody
    public String todosUsuarios() {
        List<User> usuarios = userRepository.findAll();
        StringBuilder resultado = new StringBuilder("TOTAL USUARIOS EN BD: " + usuarios.size() + "<br><br>");

        for (User user : usuarios) {
            resultado.append("ID: ").append(user.getId())
                    .append(" | Username: ").append(user.getUsername())
                    .append(" | Email: ").append(user.getEmail())
                    .append("<br>");
        }

        return resultado.toString();
    }
}