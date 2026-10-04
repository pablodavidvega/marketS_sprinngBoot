package com.example.MarketS.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface CloudStorageService {
    String subirArchivo(MultipartFile archivo, String carpeta) throws IOException;
    String subirBytes(byte[] bytes, String nombreOriginal, String contentType) throws IOException;

    // Best-effort: ignora URLs ajenas al bucket y no propaga errores (un archivo huérfano no
    // debe hacer fallar la operación que lo reemplazó).
    void eliminarArchivo(String url);
}
