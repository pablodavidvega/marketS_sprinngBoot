package com.example.MarketS.carga.usuarios.strategy;

import com.example.MarketS.carga.usuarios.dto.DTOResultadoCargaUsuarios;
import com.example.MarketS.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ArchivoUsuariosStrategy {
    List<User> leerArchivo(MultipartFile file, DTOResultadoCargaUsuarios resultado);
}
