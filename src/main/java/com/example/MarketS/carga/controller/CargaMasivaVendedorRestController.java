package com.example.MarketS.carga.controller;

import com.example.MarketS.carga.dto.DTOResultadoCarga;
import com.example.MarketS.carga.service.CargaMasivaVendedorService;
import com.example.MarketS.model.User;
import com.example.MarketS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/vendedor/carga-masiva")
@PreAuthorize("hasAuthority('ROLE_VENDEDOR')")
@RequiredArgsConstructor
public class CargaMasivaVendedorRestController {

    private final CargaMasivaVendedorService cargaMasivaService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<DTOResultadoCarga> subirArchivo(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "zip", required = false) MultipartFile zip,
            Authentication auth) {

        if (archivo == null || archivo.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new DTOResultadoCarga(0, 0, 0,
                            List.of("Debes seleccionar un archivo CSV o Excel")));
        }

        User vendedor = userService.buscarPorEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));

        DTOResultadoCarga resultado = cargaMasivaService.procesar(archivo, zip, vendedor);
        return ResponseEntity.ok(resultado);
    }
}
