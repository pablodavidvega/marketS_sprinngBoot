package com.example.MarketS.carga.usuarios.controller;

import com.example.MarketS.carga.dto.DTOResultadoCarga;
import com.example.MarketS.carga.usuarios.service.CargaMasivaUsuariosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/carga-usuarios")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class CargaMasivaUsuariosRestController {

    private final CargaMasivaUsuariosService cargaMasivaUsuariosService;

    @PostMapping
    public ResponseEntity<DTOResultadoCarga> subirArchivo(
            @RequestParam("archivo") MultipartFile archivo) {

        if (archivo == null || archivo.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new DTOResultadoCarga(0, 0, 0,
                            List.of("Debes seleccionar un archivo CSV o Excel")));
        }

        DTOResultadoCarga resultado = cargaMasivaUsuariosService.procesar(archivo);
        return ResponseEntity.ok(resultado);
    }
}
