package com.example.MarketS.exception;

import org.springframework.http.HttpStatus;

// El usuario autenticado no tiene permiso para esta acción puntual (distinto de un 401 por no
// estar autenticado, que Spring Security resuelve antes de llegar a un controller).
public class NoAutorizadoException extends NegocioException {
    public NoAutorizadoException(String mensaje) {
        super(HttpStatus.FORBIDDEN, mensaje);
    }
}
