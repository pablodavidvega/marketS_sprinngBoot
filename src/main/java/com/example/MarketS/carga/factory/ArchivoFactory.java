package com.example.MarketS.carga.factory;

import org.springframework.web.multipart.MultipartFile;

public class ArchivoFactory {

    public static String detectarTipo(MultipartFile file) {
        String nombre = file.getOriginalFilename().toLowerCase();

        if (nombre.endsWith(".csv") || nombre.endsWith(".txt")) {
            return "CSV";
        }
        if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
            return "EXCEL";
        }

        return "DESCONOCIDO";
    }
}
