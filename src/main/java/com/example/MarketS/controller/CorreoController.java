package com.example.MarketS.controller;

import com.example.MarketS.model.Correo;
import com.example.MarketS.service.CorreoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/correos")
@PreAuthorize("hasRole('ADMIN')")
public class CorreoController {

    @Autowired
    private CorreoService correoService;

    // 📧 Envío individual (por query param)
    @GetMapping
    public String enviarCorreo(@RequestParam String para, @RequestParam String asunto, @RequestParam String mensaje) {
        Correo correo = new Correo.Builder()
                .asunto(asunto)
                .mensaje(mensaje)
                .destinatarios(List.of(para))
                .build();
        correoService.enviarCorreo(correo);
        return "Correo enviado a: " + para;
    }

    // 📤 Envío masivo desde CSV
    @PostMapping("/masivo")
    public ResponseEntity<String> enviarCorreoMasivo(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam String asunto,
            @RequestParam String mensaje) {
        try {
            List<String> destinatarios = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(archivo.getInputStream()))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    if (!linea.isBlank()) destinatarios.add(linea.trim());
                }
            }

            Correo correo = new Correo.Builder()
                    .asunto(asunto)
                    .mensaje(mensaje)
                    .destinatarios(destinatarios)
                    .build();
            correoService.enviarCorreo(correo);

            return ResponseEntity.ok("Correos enviados a " + destinatarios.size() + " destinatarios.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al enviar correos: " + e.getMessage());
        }
    }

    // ✉️ Envío a varios destinatarios ingresados manualmente
    @PostMapping("/varios")
    public ResponseEntity<String> enviarCorreoVarios(
            @RequestParam(value = "destinatarios", required = false) String destinatarios,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam String asunto,
            @RequestParam String mensaje) {

        List<String> listaEmails = new ArrayList<>();

        if (destinatarios != null && !destinatarios.isBlank()) {
            Arrays.stream(destinatarios.split("[,\\n]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(listaEmails::add);
        }

        if (archivo != null && !archivo.isEmpty()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(archivo.getInputStream()))) {
                String linea;
                while ((linea = reader.readLine()) != null) {
                    if (!linea.isBlank()) listaEmails.add(linea.trim());
                }
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body("Error leyendo CSV: " + e.getMessage());
            }
        }

        Correo correo = new Correo.Builder()
                .asunto(asunto)
                .mensaje(mensaje)
                .destinatarios(listaEmails)
                .build();

        correoService.enviarCorreo(correo);
        return ResponseEntity.ok("Correos enviados a " + listaEmails.size() + " destinatarios.");
    }
}
