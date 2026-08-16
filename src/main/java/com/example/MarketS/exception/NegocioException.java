package com.example.MarketS.exception;

import org.springframework.http.HttpStatus;

// Base para excepciones de negocio que ya saben a qué HttpStatus traducirse: permite
// centralizar la conversión a respuesta HTTP en un único @ExceptionHandler(NegocioException.class)
// en GlobalExceptionHandler, en vez de un handler (y un catch ad-hoc en cada controller) por
// cada tipo de error de negocio.
public abstract class NegocioException extends RuntimeException {

    private final HttpStatus status;

    protected NegocioException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
