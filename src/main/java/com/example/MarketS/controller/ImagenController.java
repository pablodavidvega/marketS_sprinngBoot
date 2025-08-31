package com.example.MarketS.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/imagenes")
public class ImagenController {

    @GetMapping("/{nombre}")
    public ResponseEntity<Resource> verImagen(@PathVariable String nombre) throws IOException {
        Path ruta = Paths.get("C:/uploads/hana/").resolve(nombre);
        if (!Files.exists(ruta)) {
            return ResponseEntity.notFound().build();
        }
        Resource recurso = new UrlResource(ruta.toUri());
        String contentType = Files.probeContentType(ruta);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(recurso);
    }
}

