package com.example.MarketS.carga.service;

import com.example.MarketS.carga.dto.DTOResultadoCarga;
import com.example.MarketS.carga.factory.CargaArchivoFactory;
import com.example.MarketS.carga.strategy.CargaArchivoStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CargaArchivoService {

    private final CargaArchivoFactory archivoFactory;

    public DTOResultadoCarga procesarArchivo(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return new DTOResultadoCarga(0, 0, 0,
                    java.util.List.of("No se recibió ningún archivo"));
        }

        // Seleccionar estrategia según extensión
        CargaArchivoStrategy strategy = archivoFactory.obtenerEstrategia(file);

        if (strategy == null) {
            return new DTOResultadoCarga(0, 0, 0,
                    java.util.List.of("Formato no soportado"));
        }

        // Procesar con la estrategia correspondiente
        return strategy.procesar(file);
    }
}
