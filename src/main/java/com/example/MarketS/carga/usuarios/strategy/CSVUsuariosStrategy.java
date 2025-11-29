package com.example.MarketS.carga.usuarios.strategy;

import com.example.MarketS.carga.usuarios.dto.DTOResultadoCargaUsuarios;
import com.example.MarketS.model.User;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class CSVUsuariosStrategy implements ArchivoUsuariosStrategy {

    @Override
    public List<User> leerArchivo(MultipartFile file, DTOResultadoCargaUsuarios resultado) {

        List<User> usuarios = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

            String linea;
            int fila = 0; // Cambiado a 0 para contar desde la primera línea

            while ((linea = br.readLine()) != null) {
                fila++;

                // Saltar línea vacía
                if (linea.trim().isEmpty()) continue;

                // Usar split que maneje comas y punto y coma
                String[] datos = linea.split("\\s*[;,]\\s*");

                // Tu CSV tiene 7 columnas: id,username,email,password,phone,address,enabled
                // Pero el id probablemente es autoincrement, así que ignoramos la primera columna
                if (datos.length < 7) {
                    resultado.getErrores().add("Fila " + fila + " incompleta. Se esperaban 7 columnas, se encontraron: " + datos.length);
                    resultado.setFallidos(resultado.getFallidos() + 1);
                    continue;
                }

                try {
                    User u = new User();

                    // Ignorar la primera columna (id) si es autoincrement
                    u.setUsername(datos[1].trim());  // Columna 2: username
                    u.setEmail(datos[2].trim());     // Columna 3: email
                    u.setPassword(datos[3].trim());  // Columna 4: password
                    u.setPhone(datos[4].trim());     // Columna 5: phone
                    u.setAddress(datos[5].trim());   // Columna 6: address

                    // Columna 7: enabled - manejar diferentes formatos
                    String activeStr = datos[6].trim();
                    boolean active = activeStr.equals("1") || activeStr.equalsIgnoreCase("true") || activeStr.equalsIgnoreCase("activo");
                    u.setActive(active);

                    usuarios.add(u);
                    resultado.setProcesados(resultado.getProcesados() + 1);

                } catch (Exception e) {
                    resultado.getErrores().add("Error en fila " + fila + ": " + e.getMessage());
                    resultado.setFallidos(resultado.getFallidos() + 1);
                }
            }

        } catch (Exception e) {
            resultado.getErrores().add("Error leyendo archivo: " + e.getMessage());
        }

        return usuarios;
    }
}
