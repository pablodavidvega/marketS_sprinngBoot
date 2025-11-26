package com.example.MarketS.carga.strategy;

import com.example.MarketS.carga.dto.DTOResultadoCarga;
import com.example.MarketS.model.Producto;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component("csvStrategy")
public class CsvProductoCargaStrategy implements CargaArchivoStrategy {

    @Override
    public DTOResultadoCarga procesar(MultipartFile file) {

        List<String> errores = new ArrayList<>();
        List<Producto> productos = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

            String linea;
            int fila = 0;

            while ((linea = br.readLine()) != null) {

                if (fila == 0) { fila++; continue; }

                try {
                    // Admite ";" o ","
                    String[] datos = linea.split("[;,]");

                    Producto p = new Producto();
                    p.setNombre(datos[0]);
                    p.setPrecio(Double.parseDouble(datos[1]));
                    p.setDescripcion(datos[2]);
                    p.setImagen(datos[3]);
                    p.setStock(Integer.parseInt(datos[4]));

                    productos.add(p);

                } catch (Exception e) {
                    errores.add("Error en fila " + fila + ": " + e.getMessage());
                }

                fila++;
            }

        } catch (Exception e) {
            return new DTOResultadoCarga(0, 0, 0,
                    List.of("No se pudo leer el CSV: " + e.getMessage()));
        }

        return new DTOResultadoCarga(productos.size(), productos.size(), errores.size(), errores);
    }
}
