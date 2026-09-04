package com.example.MarketS.service.impl;

import com.example.MarketS.exception.ArchivoInvalidoException;
import com.example.MarketS.service.CloudStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

@Service
public class SupabaseStorageServiceImpl implements CloudStorageService {

    @Value("${supabase.url:https://wsnfaxuxgbtzefhexhyi.supabase.co}")
    private String supabaseUrl;

    @Value("${supabase.key:}")
    private String supabaseKey;

    @Value("${supabase.bucket:productos}")
    private String bucketName;

    private final WebClient webClient;

    public SupabaseStorageServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public String subirArchivo(MultipartFile archivo, String carpeta) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }

        // Si no hay API key configurada, podemos avisar o retornar null para evitar colisiones
        if (supabaseKey == null || supabaseKey.isBlank()) {
            throw new IllegalStateException("La propiedad 'supabase.key' no está configurada. Por favor añádela en tu application.properties");
        }

        byte[] contenido = archivo.getBytes();
        if (!ArchivoMultimediaValidator.esImagenOVideoValido(contenido)) {
            throw new ArchivoInvalidoException("El archivo no es una imagen o video válido");
        }

        String originalFilename = archivo.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : "";

        // Generar un nombre de archivo limpio y único. archivo.getName() es el nombre del CAMPO
        // multipart (no el filename), fijo por el @RequestParam de cada caller — pero se sanea
        // igual que en subirBytes (whitelist, no solo espacios) para no depender de que ningún
        // caller futuro pase acá un nombre de campo dinámico sin validar.
        String nombreLimpio = archivo.getName().replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
        String nombreArchivo = System.currentTimeMillis() + "_" + nombreLimpio + extension;

        // Construir la ruta final (ej: carpeta/nombre_archivo.jpg)
        String rutaArchivo = nombreArchivo;
        if (carpeta != null && !carpeta.isBlank()) {
            rutaArchivo = carpeta.replaceAll("^/|/$", "") + "/" + nombreArchivo;
        }

        String uploadUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, rutaArchivo);

        try {
            // Subir archivo a Supabase usando WebClient
            webClient.post()
                    .uri(uploadUrl)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.parseMediaType(archivo.getContentType() != null ? archivo.getContentType() : "application/octet-stream"))
                    .bodyValue(contenido)
                    .retrieve()
                    .toBodilessEntity()
                    .block(); // Síncrono para el hilo del controlador de Spring Boot

            // Retornar la URL pública del archivo
            return String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucketName, rutaArchivo);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            System.err.println("❌ ERROR DETALLADO DESDE SUPABASE STORAGE: " + e.getResponseBodyAsString());
            throw new IOException("Error al subir archivo a Supabase Storage: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            System.err.println("❌ ERROR GENERICO EN SUBIDA: " + e.getMessage());
            throw new IOException("Error al subir archivo a Supabase Storage: " + e.getMessage(), e);
        }
    }

    @Override
    public String subirBytes(byte[] bytes, String nombreOriginal, String contentType) throws IOException {
        if (bytes == null || bytes.length == 0) return null;

        if (supabaseKey == null || supabaseKey.isBlank()) {
            throw new IllegalStateException("La propiedad 'supabase.key' no está configurada.");
        }

        if (!ArchivoMultimediaValidator.esImagenOVideoValido(bytes)) {
            throw new ArchivoInvalidoException("El archivo '" + nombreOriginal + "' no es una imagen o video válido");
        }

        String nombreBase = nombreOriginal != null ? nombreOriginal : "archivo";
        String extension = "";
        int lastDot = nombreBase.lastIndexOf('.');
        if (lastDot > 0) {
            extension = nombreBase.substring(lastDot);
            nombreBase = nombreBase.substring(0, lastDot).replaceAll("[^a-zA-Z0-9_-]", "_");
        } else {
            nombreBase = nombreBase.replaceAll("[^a-zA-Z0-9_-]", "_");
        }
        String nombreFinal = System.currentTimeMillis() + "_" + nombreBase + extension;

        String uploadUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, nombreFinal);

        try {
            webClient.post()
                    .uri(uploadUrl)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                    .bodyValue(bytes)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucketName, nombreFinal);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            throw new IOException("Error al subir bytes a Supabase: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new IOException("Error al subir bytes a Supabase: " + e.getMessage(), e);
        }
    }
}
