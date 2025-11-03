package com.example.MarketS.controller;

import com.example.MarketS.factory.CorreoFactory;
import com.example.MarketS.model.Correo;
import com.example.MarketS.service.CorreoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/enviar-correo")
public class CorreoController {

    @Autowired
    private CorreoService correoService;

    @GetMapping
    public String enviarCorreo(@RequestParam String para) {
        Correo correoPrueba = CorreoFactory.crearCorreoPrueba(para);
        correoService.enviarCorreo(correoPrueba);
        return "Correo enviado a: " + para;
    }

    @PostMapping("/masivo")
    public ResponseEntity<String> enviarCorreoMasivo(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("asunto") String asunto,
            @RequestParam("mensaje") String mensaje) {
        try {
            List<String> destinatarios = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(archivo.getInputStream()))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    if (!linea.isBlank()) destinatarios.add(linea.trim());
                }
            }

            Correo correoMasivo = CorreoFactory.crearCorreoMasivo(destinatarios, asunto, mensaje);
            correoService.enviarCorreo(correoMasivo);

            return ResponseEntity.ok("Correos enviados correctamente a " + destinatarios.size() + " destinatarios.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al enviar correos: " + e.getMessage());
        }
    }
}
