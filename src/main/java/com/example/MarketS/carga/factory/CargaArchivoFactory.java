package com.example.MarketS.carga.factory;

import com.example.MarketS.carga.strategy.CargaArchivoStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class CargaArchivoFactory {

    private final CargaArchivoStrategy csvStrategy;
    private final CargaArchivoStrategy excelStrategy;

    public CargaArchivoFactory(
            @Qualifier("csvStrategy") CargaArchivoStrategy csvStrategy,
            @Qualifier("excelStrategy") CargaArchivoStrategy excelStrategy
    ) {
        this.csvStrategy = csvStrategy;
        this.excelStrategy = excelStrategy;
    }

    public CargaArchivoStrategy obtenerEstrategia(MultipartFile archivo) {

        String nombre = archivo.getOriginalFilename().toLowerCase();

        if (nombre.endsWith(".csv") || nombre.endsWith(".txt")) {
            return csvStrategy;
        }

        if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
            return excelStrategy;
        }

        return null;
    }
}
