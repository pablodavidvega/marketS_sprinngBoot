package com.example.MarketS.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface CloudStorageService {
    String subirArchivo(MultipartFile archivo, String carpeta) throws IOException;
}
