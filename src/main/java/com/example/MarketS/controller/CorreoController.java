package com.example.MarketS.controller;

import com.example.MarketS.service.CorreoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/enviar-correo")
public class CorreoController {

    private static final Logger logger = LoggerFactory.getLogger(CorreoController.class);

    @Autowired
    private CorreoService correoService;

    // 🟢 Envío individual de prueba
    @GetMapping
    public String enviarCorreo(@RequestParam String para) {
        logger.info("📨 Petición recibida para enviar correo a {}", para);

        String asunto = "Correo de prueba desde MarketS";
        String texto = "Hola, este es un correo de prueba enviado desde la app MarketS.";

        correoService.enviarCorreo(para, asunto, texto);

        logger.info("✅ Solicitud de envío procesada para {}", para);
        return "Correo enviado a: " + para;
    }

    // 🟣 Envío masivo con CSV
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

            correoService.enviarCorreoMasivo(destinatarios, asunto, mensaje);
            logger.info("📤 Correos masivos enviados a {} destinatarios", destinatarios.size());
            return ResponseEntity.ok("Correos enviados correctamente a " + destinatarios.size() + " destinatarios.");
        } catch (Exception e) {
            logger.error("❌ Error al enviar correos masivos: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error al enviar correos: " + e.getMessage());
        }
    }
}
