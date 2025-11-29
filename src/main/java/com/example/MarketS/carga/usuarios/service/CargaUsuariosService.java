package com.example.MarketS.carga.usuarios.service;

import com.example.MarketS.carga.usuarios.dto.DTOResultadoCargaUsuarios;
import com.example.MarketS.carga.usuarios.strategy.CSVUsuariosStrategy;
import com.example.MarketS.carga.usuarios.strategy.ExcelUsuariosStrategy;
import com.example.MarketS.model.Rol;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.RolRepository;
import com.example.MarketS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CargaUsuariosService {

    private final CSVUsuariosStrategy csvStrategy;
    private final ExcelUsuariosStrategy excelStrategy;
    private final UserRepository userRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public DTOResultadoCargaUsuarios procesarArchivo(MultipartFile file) {

        DTOResultadoCargaUsuarios resultado = new DTOResultadoCargaUsuarios();

        if (file.isEmpty()) {
            resultado.getErrores().add("El archivo está vacío");
            return resultado;
        }

        String nombre = file.getOriginalFilename(); // ✅ VARIABLE DECLARADA
        if (nombre == null) {
            resultado.getErrores().add("Nombre de archivo inválido");
            return resultado;
        }

        nombre = nombre.toLowerCase();

        List<User> usuarios;

        if (nombre.endsWith(".csv")) {
            usuarios = csvStrategy.leerArchivo(file, resultado);
        } else if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
            usuarios = excelStrategy.leerArchivo(file, resultado);
        } else {
            resultado.getErrores().add("Formato no válido (solo CSV o Excel)");
            return resultado;
        }

        System.out.println("DEBUG - Usuarios a procesar: " + usuarios.size());

        // REINICIAR contadores para la fase de guardado
        int exitososGuardado = 0;
        int fallidosGuardado = 0;

        Rol rolCliente = rolRepository.findById(2L).orElseThrow(() ->
                new RuntimeException("Rol con ID 2 no encontrado")
        );

        for (User u : usuarios) {
            try {
                // VERIFICAR SI EL USUARIO YA EXISTE
                if (userRepository.existsByUsername(u.getUsername())) {
                    resultado.getErrores().add("Usuario '" + u.getUsername() + "' ya existe - omitido");
                    fallidosGuardado++;
                    continue;
                }

                // VERIFICAR SI EL EMAIL YA EXISTE
                if (userRepository.existsByEmail(u.getEmail())) {
                    resultado.getErrores().add("Email '" + u.getEmail() + "' ya existe - omitido");
                    fallidosGuardado++;
                    continue;
                }

                u.setRol(rolCliente);
                u.setPassword(passwordEncoder.encode(u.getPassword()));
                u.setCreatedAt(LocalDateTime.now());
                u.setUpdatedAt(LocalDateTime.now());

                userRepository.save(u);
                exitososGuardado++;

                System.out.println("✓ Guardado usuario: " + u.getUsername());

            } catch (Exception e) {
                resultado.getErrores().add("Error guardando usuario " + u.getUsername() + ": " + e.getMessage());
                fallidosGuardado++;
            }
        }

        // ACTUALIZAR RESULTADO FINAL
        resultado.setExitosos(exitososGuardado);
        resultado.setFallidos(resultado.getFallidos() + fallidosGuardado); // Sumar fallidos de lectura + guardado

        System.out.println("DEBUG - Resultado final: " +
                "Exitosos: " + resultado.getExitosos() +
                ", Fallidos: " + resultado.getFallidos() +
                ", Procesados: " + resultado.getProcesados());

        return resultado;
    }
}