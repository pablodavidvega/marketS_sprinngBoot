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

@Controller // Indica que esta clase maneja rutas y vistas en la web
@RequiredArgsConstructor // Crea el constructor para los campos final (inyección automática)
public class CargaUsuariosController {
    //CargaUsuariosService=Tipo nombre de la clase
    // cargaUsuariosService=El nombre del objeto que voy a usar
    private final CargaUsuariosService cargaUsuariosService; // Servicio que procesa el archivo de usuarios
    private final UserRepository userRepository; // Repositorio para consultar la tabla de usuarios

    @GetMapping("/admin/carga-usuarios/form") // Ruta para mostrar el formulario de carga
    public String mostrarFormulario() {
        return "admin/carga_usuarios_form"; // Devuelve la vista con el formulario
    }

    @PostMapping("/admin/carga-usuarios/subir") // Ruta que recibe el archivo subido
    public String subir(@RequestParam("archivo") MultipartFile file, Model model) {

        if (file.isEmpty()) { // Valida si no se seleccionó un archivo
            model.addAttribute("mensaje", "Debes seleccionar un archivo."); // Mensaje de error
            return "admin/carga_usuarios_form"; // Vuelve al formulario
        }

        DTOResultadoCargaUsuarios r = cargaUsuariosService.procesarArchivo(file);
        // Procesa el archivo y devuelve resultados (procesados, fallidos, errores)

        model.addAttribute("mensaje",
                "Procesados: " + r.getProcesados() +
                        " | Errores: " + r.getFallidos()
        ); // Envía mensaje resumen al frontend

        model.addAttribute("errores", r.getErrores());
        // Envía la lista de errores para mostrarla en pantalla

        return "admin/carga_usuarios_form"; // Retorna nuevamente la vista del formulario con los resultados
    }
}
