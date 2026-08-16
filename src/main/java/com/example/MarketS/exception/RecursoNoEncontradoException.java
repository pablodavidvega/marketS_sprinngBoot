package com.example.MarketS.exception;

import org.springframework.http.HttpStatus;

// El recurso solicitado (por id, por nombre, etc.) no existe. Reemplaza al patrón disperso de
// "orElseThrow(() -> new RuntimeException(\"X no encontrado\"))" atrapado ad-hoc en cada
// controller con su propio try/catch → 400/404 genérico.
public class RecursoNoEncontradoException extends NegocioException {
    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
