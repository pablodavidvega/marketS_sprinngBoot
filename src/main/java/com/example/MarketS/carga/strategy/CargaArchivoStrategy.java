package com.example.MarketS.carga.strategy;

import com.example.MarketS.carga.dto.DTOResultadoCarga;
import org.springframework.web.multipart.MultipartFile;

public interface CargaArchivoStrategy {
    DTOResultadoCarga procesar(MultipartFile file);
}
