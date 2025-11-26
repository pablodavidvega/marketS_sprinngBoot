package com.example.MarketS.carga.strategy;

import com.example.MarketS.model.Producto;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ArchivoProductoStrategy {
    List<Producto> leerArchivo(MultipartFile file) throws Exception;
}
