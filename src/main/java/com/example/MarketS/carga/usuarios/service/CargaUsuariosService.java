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
//Spring crea automáticamente el constructor para los atributos final
@RequiredArgsConstructor
public class CargaUsuariosService {
    //Dependencias que el servicio necesita:
    //Spring las inyecta con el constructor automático.
    //    leer CSV
    private final CSVUsuariosStrategy csvStrategy;
    //    leer Excel
    private final ExcelUsuariosStrategy excelStrategy;
    //Guardar usuarios en BD
    private final UserRepository userRepository;
    //consultar roles
    private final RolRepository rolRepository;
    //encriptar contraseñas
    private final PasswordEncoder passwordEncoder;

    //Método principal que recibe el archivo cargado y devuelve el resultado.
    public DTOResultadoCargaUsuarios procesarArchivo(MultipartFile file) {
        //Objeto donde se guardan errores, éxitos, procesados, etc.
        DTOResultadoCargaUsuarios resultado = new DTOResultadoCargaUsuarios();
        //Condicional si el archivo viene vacío
        //Retorna el resultado con un error y no sigue.
        if (file.isEmpty()) {
            resultado.getErrores().add("El archivo está vacío");
            return resultado;
        }
        //Obtiene el nombre del archivo que subió el usuario
        String nombre = file.getOriginalFilename();
        //Si el archivo no tiene nombre, se detiene
        if (nombre == null) {
            resultado.getErrores().add("Nombre de archivo inválido");
            return resultado;
        }
        //Convierte el nombre a minúsculas para comparar extensiones
        nombre = nombre.toLowerCase();
        //Lista donde se guardarán los usuarios leídos del archivo.
        List<User> usuarios;
        //Condicional donde el sistema decide cómo leer el archivo según su extensión
        if (nombre.endsWith(".csv")) {
            usuarios = csvStrategy.leerArchivo(file, resultado);
        } else if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
            usuarios = excelStrategy.leerArchivo(file, resultado);
        } else {
            resultado.getErrores().add("Formato no válido (solo CSV o Excel)");
            return resultado;
        }
        //Muestra en consola cuántos usuarios trae el archivo.
        System.out.println("DEBUG - Usuarios a procesar: " + usuarios.size());

        // Cuántos usuarios se guardaron correctamente y cuántos fallaron durante el proceso de guardado en la base de datos. de guardado
        int exitososGuardado = 0;
        int fallidosGuardado = 0;
        //Busca el rol cliente (ID = 2).
        //Si no existe, el programa falla.
        Rol rolCliente = rolRepository.findById(2L).orElseThrow(() ->
                new RuntimeException("Rol con ID 2 no encontrado")
        );
        //Recorre uno por uno los usuarios leídos del archivo.
        for (User u : usuarios) {
            try {
                // Si el nombre de usuario ya existe Lo salta y lo marca como error.
                if (userRepository.existsByUsername(u.getUsername())) {
                    resultado.getErrores().add("Usuario '" + u.getUsername() + "' ya existe - omitido");
                    fallidosGuardado++;
                    continue;
                }

                // Si el correo ya existe Lo omite y sigue con el siguiente
                if (userRepository.existsByEmail(u.getEmail())) {
                    resultado.getErrores().add("Email '" + u.getEmail() + "' ya existe - omitido");
                    fallidosGuardado++;
                    continue;
                }
                //Le asigna rol cliente por defecto
                u.setRol(rolCliente);
                //Encripta la contraseña.
                u.setPassword(passwordEncoder.encode(u.getPassword()));
                //Crea fechas de creación y actualización
                u.setCreatedAt(LocalDateTime.now());
                u.setUpdatedAt(LocalDateTime.now());
                //Guarda al usuario y suma uno a los exitosos
                userRepository.save(u);
                exitososGuardado++;

                System.out.println("✓ Guardado usuario: " + u.getUsername());
            //Si algo falla al guardar, lo marca como error
            //Captura cualquier error inesperado que ocurra mientras se guarda un usuario en la base de datos
            } catch (Exception e) {
                resultado.getErrores().add("Error guardando usuario " + u.getUsername() + ": " + e.getMessage());
                fallidosGuardado++;
            }
        }

        //Guarda en el objeto resultado cuántos usuarios se guardaron bien en la base de datos.
        resultado.setExitosos(exitososGuardado);
        //Suma dos tipos de fallos: Fallos de lectura, y fallos de guardado
        resultado.setFallidos(resultado.getFallidos() + fallidosGuardado); // Sumar fallidos de lectura + guardado
        //Imprime en consola el resumen final
        System.out.println("DEBUG - Resultado final: " +
                "Exitosos: " + resultado.getExitosos() +
                ", Fallidos: " + resultado.getFallidos() +
                ", Procesados: " + resultado.getProcesados());
        //Devuelve el resultado al controlador.
        return resultado;
    }
}